package com.sunlee.sys.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码、发票流水、库存/单据锁。Redis 可用时写入 Redis，否则回退内存或仅靠数据库条件更新。
 */
@Slf4j
@Service
public class RedisBiz {

    public static final String CAPTCHA_PREFIX = "captcha:";
    public static final String INVOICE_SEQ_PREFIX = "invoice:seq:";

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final Duration INVOICE_TTL = Duration.ofDays(3);

    private final StringRedisTemplate redis;
    private final ConcurrentHashMap<String, String> local = new ConcurrentHashMap<>();
    private volatile Boolean available;

    public RedisBiz(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redis = redisProvider.getIfAvailable();
    }

    public boolean available() {
        if (available != null) {
            return available;
        }
        if (redis == null || redis.getConnectionFactory() == null) {
            available = false;
            log.warn("未注入 Redis，验证码/发票流水使用内存");
            return false;
        }
        try (var connection = redis.getConnectionFactory().getConnection()) {
            String pong = connection.ping();
            available = pong != null && pong.equalsIgnoreCase("PONG");
            if (Boolean.TRUE.equals(available)) {
                log.info("Redis 已连接，验证码/发票流水走 Redis");
            } else {
                log.warn("Redis ping 失败，验证码/发票流水使用内存");
            }
        } catch (Exception e) {
            available = false;
            log.warn("Redis 不可用，验证码/发票流水使用内存: {}", e.getMessage());
        }
        return Boolean.TRUE.equals(available);
    }

    public void saveCaptcha(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return;
        }
        String key = CAPTCHA_PREFIX + captchaId;
        if (writeRedis(key, code, CAPTCHA_TTL)) {
            return;
        }
        local.put(key, code);
    }

    public boolean consumeCaptcha(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }
        String key = CAPTCHA_PREFIX + captchaId;
        String cached = take(key);
        return cached != null && cached.equalsIgnoreCase(code);
    }

    /**
     * 抢锁。Redis 不可用时返回 true，由数据库条件更新兜底。
     */
    public boolean tryLock(String key, Duration ttl) {
        if (key == null || !available()) {
            return true;
        }
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            available = false;
            log.warn("Redis 抢锁失败，回退仅靠数据库: {}", e.getMessage());
            return true;
        }
    }

    public boolean tryLockWait(String key, Duration ttl, Duration wait, Duration retry) {
        long deadline = System.currentTimeMillis() + wait.toMillis();
        while (true) {
            if (tryLock(key, ttl)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(Math.max(10, retry.toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public void unlock(String key) {
        if (key == null || !available()) {
            return;
        }
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("Redis 解锁失败（将靠 TTL 过期）: {}", e.getMessage());
        }
    }

    /**
     * @return 当日下一流水；Redis 不可用时返回 -1，由调用方回退库表取号
     */
    public long nextInvoiceSeq(LocalDate bizDate) {
        if (bizDate == null || !available()) {
            return -1L;
        }
        String key = INVOICE_SEQ_PREFIX + bizDate.format(DAY);
        try {
            Long seq = redis.opsForValue().increment(key);
            redis.expire(key, INVOICE_TTL);
            return seq == null || seq < 1 ? 1L : seq;
        } catch (Exception e) {
            available = false;
            log.warn("发票流水 Redis 失败，回退库表: {}", e.getMessage());
            return -1L;
        }
    }

    private boolean writeRedis(String key, String value, Duration ttl) {
        if (!available()) {
            return false;
        }
        try {
            redis.opsForValue().set(key, value, ttl);
            return true;
        } catch (Exception e) {
            available = false;
            log.warn("写入 Redis 失败，回退内存: {}", e.getMessage());
            return false;
        }
    }

    private String take(String key) {
        if (available()) {
            try {
                String cached = redis.opsForValue().get(key);
                redis.delete(key);
                return cached;
            } catch (Exception e) {
                available = false;
                log.warn("读取 Redis 失败，回退内存: {}", e.getMessage());
            }
        }
        return local.remove(key);
    }
}
