package com.github.verils.dlock.redis;

public interface RedisClient {

    boolean tryAcquire(String key, String value, int expireInSeconds);

    String getLock(String key);

    void release(String key);
}
