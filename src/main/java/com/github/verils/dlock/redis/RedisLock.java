package com.github.verils.dlock.redis;

import com.github.verils.dlock.DistributedLock;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

/**
 * 不可重入的分布式锁，基于Jedis实现
 */
@Slf4j
public class RedisLock implements DistributedLock {

    private final Sync sync = new Sync();

    private final RedisClient redis;

    private final String key;
    private final int expireSeconds;
    private final int waitSeconds;

    private String value;

    public RedisLock(RedisClient redis, String key, int expireSeconds) {
        this(redis, key, expireSeconds, 30);
    }

    public RedisLock(RedisClient redis, String key, int expireSeconds, int waitSeconds) {
        this.redis = redis;
        this.key = key;
        this.expireSeconds = expireSeconds;
        this.waitSeconds = waitSeconds;
    }

    @Override
    public void lock() {
        sync.acquire(1);
        try {
            acquire();
        } catch (InterruptedException e) {
            sync.release(1);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            sync.release(1);
            throw e;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
        try {
            acquire();
        } catch (Exception e) {
            sync.release(1);
            throw e;
        }
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
//        try {
//            return tryLock(expireSeconds, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            throw new IllegalThreadStateException();
//        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
//        boolean locked = sync.tryAcquireNanos(1, unit.toNanos(time));
//        if (locked) {
//            locked = tryAcquire();
//            if (!locked) {
//                sync.release(1);
//            }
//        }
//        return locked;
    }

    @Override
    public void unlock() {
        if (!sync.isHeldExclusively()) {
            throw new IllegalMonitorStateException("Current thread is not holding lock");
        }
        if (release()) {
            sync.release(1);
        }
    }

    @Override
    public Condition newCondition() {
        return sync.newConditionObject();
    }

    /**
     * 该方法是线程安全的
     */
    private void acquire() throws InterruptedException {
        String value = getLock();
        while (!redis.tryAcquire(key, value, expireSeconds)) {
            Thread.sleep(waitSeconds);
        }
        this.value = value;
    }

    private boolean tryAcquire() {
        throw new UnsupportedOperationException();
//        String value = getLock();
//        boolean acquired = redis.tryAcquire(key, value, expireSeconds);
//        if (acquired) {
//            this.value = value;
//        }
//        return acquired;
    }

    /**
     * 该方法是线程安全的
     *
     * @return true成功删除redis中的锁
     */
    private boolean release() {
        if (value == null) {
            throw new IllegalMonitorStateException();
        }
        boolean released = redis.tryRelease(key, value);
        if (released) {
            this.value = null;
        }
        return released;
    }

    private String getLock() {
        return UUID.randomUUID().toString();
    }

    /**
     * 利用同步器来保证进程内的线程安全
     */
    private class Sync extends AbstractQueuedSynchronizer {

        @Override
        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        public boolean tryRelease(int acquires) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return Thread.currentThread() == getExclusiveOwnerThread();
        }

        Condition newConditionObject() {
            return new ConditionObject();
        }
    }
}
