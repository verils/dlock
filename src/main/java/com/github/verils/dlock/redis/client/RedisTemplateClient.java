package com.github.verils.dlock.redis.client;

import com.github.verils.dlock.redis.RedisClient;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisCommands;

public class RedisTemplateClient implements RedisClient {

    private static final String STATUS_OK = "OK";

    private final StringRedisTemplate redisTemplate;

    public RedisTemplateClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, String value, int expireSeconds) {
        return redisTemplate.execute((RedisConnection connection) -> retrieveLock(connection, key, value, expireSeconds));
    }

    @Override
    public boolean canRelease(String key, String value) {
        String lock = redisTemplate.opsForValue().get(key);
        if (lock == null) {
            throw new IllegalMonitorStateException();
        }
        return lock.equals(value);
    }

    @Override
    public void release(String key) {
        redisTemplate.delete(key);
    }

    private boolean retrieveLock(RedisConnection connection, String key, String value, int expireSeconds) {
        Object nativeConnection = connection.getNativeConnection();
        if (nativeConnection instanceof JedisCommands) {
            JedisCommands jedisCommands = (JedisCommands) nativeConnection;
            String status = jedisCommands.set(key, value, "NX", "EX", expireSeconds);
            return STATUS_OK.equals(status);
        }
        throw new IllegalStateException(String.format("不支持的NativeConnection类型: %s", nativeConnection.getClass()));
    }
}
