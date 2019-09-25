package com.github.verils.example.dlock.redis;

public interface RedisClient {

    boolean tryAcquire(String key, String lock, int expireSeconds);

    void tryRelease(String key, String value);
}
