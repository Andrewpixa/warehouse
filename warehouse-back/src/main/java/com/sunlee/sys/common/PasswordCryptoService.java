package com.sunlee.sys.common;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.BCrypt;
import com.sunlee.sys.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
/**
 * 登录密码：RSA 传输解密 + BCrypt 存储；兼容历史 MD5+salt。
 */
@Slf4j
@Component
public class PasswordCryptoService {

    private RSA rsa;
    private String publicKeyPem;

    @PostConstruct
    public void init() {
        // 2048 位更稳妥；与前端 JSEncrypt（PKCS1）配合
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 2048);
        this.rsa = new RSA(keyPair.getPrivate(), keyPair.getPublic());
        String publicKeyBase64 = rsa.getPublicKeyBase64();
        this.publicKeyPem = toPem("PUBLIC KEY", publicKeyBase64);
        log.info("RSA 登录密钥已生成（2048 位，仅下发公钥）");
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    /**
     * 解密前端 RSA 密文；若本身已是短明文（兼容旧客户端）则原样返回。
     */
    public String decryptIncomingPassword(String cipherOrPlain) {
        if (StringUtils.isBlank(cipherOrPlain)) {
            return cipherOrPlain;
        }
        // 明文密码通常较短；RSA 密文为较长 Base64
        if (cipherOrPlain.length() < 80 && !cipherOrPlain.contains("\n")) {
            return cipherOrPlain;
        }
        try {
            return rsa.decryptStr(cipherOrPlain, KeyType.PrivateKey);
        } catch (Exception e) {
            log.warn("RSA 密码解密失败: {}", e.getMessage());
            throw new IllegalArgumentException("密码解密失败，请刷新页面后重试");
        }
    }

    public boolean matches(String plainPassword, User user) {
        if (user == null || StringUtils.isBlank(plainPassword) || StringUtils.isBlank(user.getPwd())) {
            return false;
        }
        String stored = user.getPwd();
        if (isBcrypt(stored)) {
            return BCrypt.checkpw(plainPassword, stored);
        }
        return md5Hash(plainPassword, user.getSalt(), Constast.HASHITERATIONS).equals(stored);
    }

    public String encodeBcrypt(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public boolean isBcrypt(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    /**
     * 历史 MD5 用户登录成功后，静默升级为 BCrypt（salt 清空）。
     */
    public void upgradeLegacyHashIfNeeded(User user, String plainPassword) {
        if (user == null || isBcrypt(user.getPwd())) {
            return;
        }
        user.setPwd(encodeBcrypt(plainPassword));
        user.setSalt(null);
    }

    public String md5Hash(String source, String salt, int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] saltBytes = salt == null ? new byte[0] : salt.getBytes(StandardCharsets.UTF_8);
            byte[] sourceBytes = source.getBytes(StandardCharsets.UTF_8);
            md.update(saltBytes);
            byte[] hash = md.digest(sourceBytes);
            for (int i = 1; i < iterations; i++) {
                hash = md.digest(hash);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String toPem(String type, String base64) {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            sb.append(base64, i, end).append('\n');
        }
        sb.append("-----END ").append(type).append("-----");
        return sb.toString();
    }
}
