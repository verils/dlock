package com.github.verils.dlock.redis.client;

import com.github.verils.dlock.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
public class JedisClient implements RedisClient {

    private static final String STATUS_OK = "OK";

    private static final String NX = "NX";
    private static final String EX = "EX";

    private final JedisPool jedisPool;

    public JedisClient(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean tryAcquire(String key, String value, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String status = jedis.set(key, value, NX, EX, expireSeconds);
            boolean acquired = STATUS_OK.equals(status);
            if (log.isDebugEnabled()) {
                if (acquired) {
                    log.debug("Acquired lock [{} -\"{}\"]", key, value);
                }
            }
            return acquired;
        }
    }

    @Override
    public boolean canRelease(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            String lock = jedis.get(key);
            if (lock == null) {
                throw new IllegalMonitorStateException();
            }
            return lock.equals(value);
        }
    }

    @Override
    public void release(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
            if (log.isDebugEnabled()) {
                log.info("Released lock [{}]", key);
            }
        }
    }
}