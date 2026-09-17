package com.sunlee.sys.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @Author: sunlee
 * @Date: 2026/04/10 23:44
 */
@Slf4j
public class AppFileUtils {

    /**
     * 文件上传的保存路径  默认值
     */
    public static String UPLOAD_PATH = "/opt/warehouse/upload/";

    static {
        //通过反射的方式，读取配置文件的存储地址
        InputStream stream = AppFileUtils.class.getClassLoader().getResourceAsStream("file.properties");
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            log.error("操作失败: {}", e.getMessage(), e);
        }
        String property = properties.getProperty("filepath");
        if (null != property) {
            UPLOAD_PATH = property;
        }
        File dir = new File(UPLOAD_PATH);
        if (!dir.isDirectory() || !dir.canWrite()) {
            File local = new File(System.getProperty("user.dir"), "upload");
            local.mkdirs();
            if (local.isDirectory() && local.canWrite()) {
                UPLOAD_PATH = local.getAbsolutePath() + File.separator;
                log.info("upload path fallback to {}", UPLOAD_PATH);
            }
        }
    }

    /**
     * 根据文件老名字得到新名字
     *
     * @param oldName 文件老名字
     * @return 新名字由32位随机数加图片后缀组成
     */
    public static String createNewFileName(String oldName) {
        //获取文件名后缀，无后缀时使用 .tmp 作为默认后缀防止 NPE
        int lastDot = oldName.lastIndexOf(".");
        String stuff;
        if (lastDot == -1 || lastDot == oldName.length() - 1) {
            stuff = ".tmp";
        } else {
            stuff = oldName.substring(lastDot);
        }
        //将UUID与文件名后缀进行拼接，生成新的文件名  生成的UUID为32位
        return IdUtil.simpleUUID().toUpperCase() + stuff;
    }

    /**
     * 文件下载
     *
     * @param path
     * @return
     */
    public static ResponseEntity<Object> createResponseEntity(String path) {
        // 1.规范化路径并校验，防止路径遍历攻击
        File baseDir = new File(UPLOAD_PATH);
        File file;
        try {
            Path basePath = baseDir.toPath().normalize().toAbsolutePath();
            Path requestedPath = new File(baseDir, path).toPath().normalize().toAbsolutePath();
            // 校验请求路径是否在允许的目录内
            if (!requestedPath.startsWith(basePath)) {
                log.warn("检测到路径遍历攻击尝试: path={}, basePath={}", path, basePath);
                return ResponseEntity.badRequest().build();
            }
            file = requestedPath.toFile();
        } catch (Exception e) {
            log.error("路径解析失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }

        // 2.校验文件是否存在且为普通文件
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        // 3.将下载的文件，封装byte[]
        byte[] bytes = null;
        try {
            bytes = FileUtil.readBytes(file);
        } catch (Exception e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
        }
        // 创建封装响应头信息的对象
        HttpHeaders header = new HttpHeaders();
        // 根据文件扩展名设置正确的Content-Type
        String contentType = getContentType(path);
        header.setContentType(MediaType.parseMediaType(contentType));
        ResponseEntity<Object> entity = new ResponseEntity<Object>(bytes, header, HttpStatus.OK);
        return entity;
    }

    public static ResponseEntity<Object> createDownloadEntity(String path, String downloadName) {
        ResponseEntity<Object> raw = createResponseEntity(path);
        if (raw.getBody() == null || raw.getStatusCode() != HttpStatus.OK) {
            return raw;
        }
        HttpHeaders header = new HttpHeaders();
        header.putAll(raw.getHeaders());
        String name = downloadName == null || downloadName.isBlank() ? path : downloadName;
        header.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename(name, java.nio.charset.StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(raw.getBody(), header, HttpStatus.OK);
    }

    /**
     * 根据文件扩展名获取Content-Type
     * 未知类型返回 application/octet-stream，避免可执行文件被伪装成图片
     */
    private static String getContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith("_temp")) {
            lower = lower.substring(0, lower.length() - 5);
        }
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html;charset=UTF-8";
        return "application/octet-stream";
    }

    /**
     * 更该图片的名字 去掉_temp
     *
     * @param goodsImg
     * @return
     */
    public static String renameFile(String goodsImg) {
        File file = new File(UPLOAD_PATH, goodsImg);
        String replace = goodsImg.replace("_temp", "");
        if (file.exists()) {
            file.renameTo(new File(UPLOAD_PATH, replace));
        }
        return replace;
    }

    /**
     * 根据老路径删除图片
     *
     * @param oldPath
     */
    public static void removeFileByPath(String oldPath) {
        if (oldPath == null || oldPath.isEmpty()) {
            return;
        }
        //图片的路径不是默认图片的路径
        if (!oldPath.equals(Constast.DEFAULT_IMG_GOODS)) {
            File file = new File(UPLOAD_PATH, oldPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }


}
