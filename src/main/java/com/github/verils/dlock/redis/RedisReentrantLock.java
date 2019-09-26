package com.github.verils.dlock.redis;

import com.github.verils.dlock.DistributedLock;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

@Slf4j
public class RedisReentrantLock implements DistributedLock {

    private final Sync sync = new Sync();

    private final RedisClient redis;

    private final String key;
    private final int defaultExpireSeconds;

    private String value;

    public RedisReentrantLock(RedisClient redis, String key, int defaultExpireSeconds) {
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
        sync.release(1);
        if (sync.released()) {
            release();
        }
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
                if (hasLock()) {
                    return;
                }
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
        } else {
            throw new IllegalMonitorStateException("Cannot unlock before retrieved lock");
        }
    }

    private boolean hasLock() {
        return value != null;
    }

    private String getLock() {
        return hasLock() ? value : UUID.randomUUID().toString();
    }

    private class Sync extends AbstractQueuedSynchronizer {

        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            } else if (Thread.currentThread() == getExclusiveOwnerThread()) {
                int newState = getState() + acquires;
                if (newState < 0) {
                    throw new Error("Maximum lock count");
                }
                setState(newState);
                return true;
            }
            return false;
        }

        public boolean tryRelease(int acquires) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                throw new IllegalMonitorStateException();
            }
            int newState = getState() - acquires;
            if (newState == 0) {
                setExclusiveOwnerThread(null);
            }
            setState(newState);
            return true;
        }

        public boolean released() {
            return getState() == 0;
        }

        public Condition newConditionObject() {
            return new ConditionObject();
        }
    }
}
