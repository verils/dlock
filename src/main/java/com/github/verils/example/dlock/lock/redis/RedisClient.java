package com.github.verils.example.dlock.lock.redis;

public interface RedisClient {

    boolean tryAcquire(String key, String lock, int expireSeconds);

    void tryRelease(String key, String value);
}
