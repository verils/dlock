//package com.github.verils.dlock.redis;
//
//import com.github.verils.dlock.DistributedLock;
//import org.springframework.data.redis.connection.RedisConnection;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.ValueOperations;
//import redis.clients.jedis.JedisCommands;
//
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.Condition;
//
//public class RedisTemplateReentrantLock implements DistributedLock {
//
//    private static final String STATUS_OK = "OK";
//
//    private final StringRedisTemplate redisTemplate;
//    private final ValueOperations<String, String> valueOperations;
//
//    private final String name;
//
//    private final long time;
//    private final TimeUnit unit;
//
//    private String value;
//    private int state;
//
//    public RedisTemplateReentrantLock(StringRedisTemplate redisTemplate, String name, long time, TimeUnit unit) {
//        this.redisTemplate = redisTemplate;
//        this.name = name;
//        this.time = time;
//        this.unit = unit;
//        this.valueOperations = this.redisTemplate.opsForValue();
//    }
//
//    @Override
//    public void lock() {
//    }
//
//    @Override
//    public void lockInterruptibly() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean tryLock() {
//        return tryLock(time, unit);
//    }
//
//    @Override
//    public boolean tryLock(long time, TimeUnit unit) {
//        boolean locked = redisTemplate.execute((RedisConnection connection) -> retrieveLock(connection, name, value, time));
//        if (locked) {
//            this.value = getLockId();
//            acquire();
//        } else {
//            String currentLock = valueOperations.get(name);
//            boolean reentrant = value.equals(currentLock);
//            if (reentrant) {
//                acquire();
//                redisTemplate.expire(name, time, unit);
//                locked = true;
//            }
//        }
//        return locked;
//    }
//
//    @Override
//    public void unlock() {
//        String currentLock = valueOperations.get(name);
//        boolean reentrant = value.equals(currentLock);
//        if (reentrant) {
//            release();
//            if (canUnlock()) {
//                this.value = null;
//                redisTemplate.delete(name);
//            }
//        }
//    }
//
//    private String getLockId() {
//        String uuid = UUID.randomUUID().toString();
//        long threadId = Thread.currentThread().getId();
//        return uuid + ":" + threadId;
//    }
//
//    private boolean retrieveLock(RedisConnection connection, String name, String value, long time) {
//        Object nativeConnection = connection.getNativeConnection();
//        if (nativeConnection instanceof JedisCommands) {
//            JedisCommands jedisCommands = (JedisCommands) nativeConnection;
//            String status = jedisCommands.set(name, value, "NX", "EX", time);
//            return STATUS_OK.equals(status);
//        }
//        throw new IllegalStateException(String.format("不支持的NativeConnection类型: %s", nativeConnection.getClass()));
//    }
//
//    private void acquire() {
//        state += 1;
//    }
//
//    private void release() {
//        state -= 1;
//    }
//
//    private boolean canUnlock() {
//        return state == 0;
//    }
//
//    @Override
//    public Condition newCondition() {
//        throw new UnsupportedOperationException();
//    }
//}