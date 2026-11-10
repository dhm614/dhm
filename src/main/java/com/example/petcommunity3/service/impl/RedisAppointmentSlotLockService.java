package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.service.AppointmentSlotLockService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RedisAppointmentSlotLockService implements AppointmentSlotLockService {
    private static final String LOCK_PREFIX = "petcommunity:appointment:slot:";
    private static final String LOCK_BUSY_MESSAGE = "该预约时间正在锁定中，请稍后再试";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    @Value("${petcommunity.appointment-lock.ttl-seconds:30}")
    private long ttlSeconds;

    public RedisAppointmentSlotLockService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    public <T> T executeWithLock(String lockKey, Callable<T> action) {
        String redisKey = LOCK_PREFIX + normalizeLockKey(lockKey);
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return executeWithLocalLock(redisKey, action);
        }

        String token = UUID.randomUUID().toString();
        boolean locked;
        try {
            locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, token, Duration.ofSeconds(Math.max(1, ttlSeconds))));
        } catch (RuntimeException ex) {
            return executeWithLocalLock(redisKey, action);
        }

        if (!locked) {
            throw new RuntimeException(LOCK_BUSY_MESSAGE);
        }

        try {
            return call(action);
        } finally {
            releaseRedisLock(redisTemplate, redisKey, token);
        }
    }

    private <T> T executeWithLocalLock(String lockKey, Callable<T> action) {
        ReentrantLock lock = localLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        boolean locked;
        try {
            locked = lock.tryLock(Math.max(1, ttlSeconds), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("预约时间锁获取被中断");
        }
        if (!locked) {
            throw new RuntimeException(LOCK_BUSY_MESSAGE);
        }
        try {
            return call(action);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                localLocks.remove(lockKey, lock);
            }
        }
    }

    private void releaseRedisLock(StringRedisTemplate redisTemplate, String redisKey, String token) {
        String scriptText = "if redis.call('get', KEYS[1]) == ARGV[1] then "
                + "return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(scriptText, Long.class);
        try {
            redisTemplate.execute(script, Collections.singletonList(redisKey), token);
        } catch (RuntimeException ignored) {
        }
    }

    private <T> T call(Callable<T> action) {
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("预约时间锁内操作失败", ex);
        }
    }

    private String normalizeLockKey(String lockKey) {
        if (!StringUtils.hasText(lockKey)) {
            return "unknown";
        }
        return lockKey.trim().replaceAll("[^a-zA-Z0-9:_-]", "_");
    }
}
