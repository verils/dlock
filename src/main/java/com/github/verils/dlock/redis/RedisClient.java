package com.github.verils.dlock.redis;

/**
 * 提供Redis访问接口，该对象可以是单例的
 */
public interface RedisClient {

    boolean tryAcquire(String key, String value, int expireInSeconds);

    void expire(String key, int expireInSeconds);

    String getLock(String key);

    void release(String key);
}
