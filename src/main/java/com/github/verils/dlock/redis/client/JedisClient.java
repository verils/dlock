package com.github.verils.dlock.redis.client;

import com.github.verils.dlock.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
public class JedisClient implements RedisClient {

    private static final String STATUS_OK = "OK";

    private static final String NX = "NX";

    /**
     * 单位：秒
     */
    private static final String EX = "EX";

    /**
     * 单位：毫秒
     */
    private static final String PX = "PX";

    private final JedisPool jedisPool;

    public JedisClient(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean tryAcquire(String key, String value, int expireInSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String status = jedis.set(key, value, NX, EX, expireInSeconds);
            boolean acquired = STATUS_OK.equals(status);
            if (acquired) {
                if (log.isDebugEnabled()) {
                    log.debug("Acquired lock [\"{}\" - \"{}\"]", key, value);
                }
            }
            return acquired;
        }
    }

    @Override
    public void expire(String key, int expireInSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(key, expireInSeconds);
        }
    }

    @Override
    public String getLock(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    @Override
    public void release(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
            if (log.isDebugEnabled()) {
                log.debug("Released lock [\"{}\"]", key);
            }
        }
    }
}
