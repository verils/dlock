package com.github.verils.dlock.redis;

public interface RedisClient {

    boolean tryAcquire(String key, String value, int expireSeconds);

    boolean canRelease(String key, String value);

    void release(String key);
}
