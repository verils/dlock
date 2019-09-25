package com.github.verils.example.dlock.redis;

import com.github.verils.example.dlock.DistributedLock;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

@Slf4j
public class RedisLock implements DistributedLock {

    private final Sync sync = new Sync();

    private final RedisClient redis;

    private final String key;
    private final int defaultExpireSeconds;

    private String value;

    public RedisLock(RedisClient redis, String key, int defaultExpireSeconds) {
        this.redis = redis;
        this.key = key;
        this.defaultExpireSeconds = defaultExpireSeconds;
    }

    @Override
    public void lock() {
        sync.acquire(1);
        acquire();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
        acquire();
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(defaultExpireSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalThreadStateException();
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        boolean locked = sync.tryAcquireNanos(1, unit.toNanos(time));
        if (locked) {
            locked = tryAcquire();
            if (!locked) {
                sync.release(1);
            }
        }
        return locked;
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
            boolean acquired = redis.tryAcquire(key, value, defaultExpireSeconds);
            if (acquired) {
                this.value = value;
                break;
            } else {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    throw new IllegalThreadStateException();
                }
            }
        }
    }

    private boolean tryAcquire() {
        String value = getLock();
        boolean acquired = redis.tryAcquire(key, value, defaultExpireSeconds);
        if (acquired) {
            this.value = value;
        }
        return acquired;
    }

    private void release() {
        if (value == null) {
            throw new IllegalMonitorStateException();
        }
        if (redis.canRelease(key, value)) {
            redis.release(key);
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
