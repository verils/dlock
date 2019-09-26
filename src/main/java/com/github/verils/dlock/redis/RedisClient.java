package com.github.verils.dlock.redis;

public interface RedisClient {

    boolean tryAcquire(String key, String value, int expireSeconds);

    boolean tryRelease(String key, String value);
}
