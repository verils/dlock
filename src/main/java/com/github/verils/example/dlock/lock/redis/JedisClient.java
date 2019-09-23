package com.github.verils.example.dlock.lock.redis;

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
            if (acquired) {
                log.info("Acquired lock [{} -\"{}\"]", key, value);
            }
            return acquired;
        }
    }

    @Override
    public void tryRelease(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            String lock = jedis.get(key);
            if (lock == null) {
                throw new IllegalMonitorStateException();
            }
            boolean canRelease = lock.equals(value);
            if (canRelease) {
                jedis.del(key);
                log.info("Released lock [{} -\"{}\"]", key, value);
            }
        }
    }
}
