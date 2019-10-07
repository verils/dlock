package com.github.verils.dlock.redis;

/**
 * 提供Redis访问接口，该对象可以是单例的
 */
public interface RedisClient {

    /**
     * 尝试获取分布式锁。具体行为是：向Redis写入一个键，同时设置过期时间。如果键不存在，插入成功；如果键已存在，则插入失败。对Redis的调用必须是原子性的
     *
     * @param key             插入Redis的键名称
     * @param value           插入Redis的键值
     * @param expireInSeconds 键的过期时间
     * @return true，表示名为{@code key}的键不存在，可以插入并成功设置了过期时间。false表示表示名为{@code key}的键存在
     */
    boolean tryAcquire(String key, String value, int expireInSeconds);

    /**
     * 重置键的过期时间
     *
     * @param key             需要重置的键名称
     * @param expireInSeconds 键的过期时间
     */
    void expire(String key, int expireInSeconds);

    /**
     * 获取键对应的值
     *
     * @param key 查询的键名称
     * @return 键对应的值，在这里代表分布式锁的唯一标识
     */
    String getLock(String key);

    /**
     * 释放分布式锁。具体行为是：删除Redis中名为{@code key}的键
     *
     * @param key 删除的键名称
     */
    void release(String key);
}
