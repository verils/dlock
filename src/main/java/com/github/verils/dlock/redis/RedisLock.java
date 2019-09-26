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
        } catch (Exception e) {
            sync.release(1);
            throw e;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
//        sync.acquireInterruptibly(1);
//        try {
//            acquire();
//        } catch (Exception e) {
//            sync.release(1);
//            throw e;
//        }
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
        release();
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newConditionObject();
    }

    private void acquire() {
        String value = getLock();
        while (true) {
            boolean acquired = redis.tryAcquire(key, value, expireSeconds);
            if (acquired) {
                this.value = value;
                break;
            } else {
                try {
                    Thread.sleep(waitSeconds);
                } catch (InterruptedException e) {
                    throw new IllegalThreadStateException();
                }
            }
        }
    }

    private boolean tryAcquire() {
        String value = getLock();
        boolean acquired = redis.tryAcquire(key, value, expireSeconds);
        if (acquired) {
            this.value = value;
        }
        return acquired;
    }

    private void release() {
        if (value == null) {
            throw new IllegalMonitorStateException("Lock state error");
        }
        if (redis.canRelease(key, value)) {
            redis.release(key);
        } else {
            throw new IllegalMonitorStateException("Cannot unlock before retrieved lock");
        }
    }

    private String getLock() {
        return UUID.randomUUID().toString();
    }

    private class Sync extends AbstractQueuedSynchronizer {

        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        public boolean tryRelease(int acquires) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public Condition newConditionObject() {
            return new ConditionObject();
        }
    }
}
