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
    private final int expireInSeconds;
    private final int waitSeconds;

    private String value;
    private int state;

    /**
     * Create a redis lock instance, when trying to acquire redis lock returns {@code false}, thread goes into self spin for a default sleep time at {@code 30ms}.
     *
     * @param redis           An {@link RedisClient} implementation providing the ability to access redis
     * @param key             To be used as the redis lock entry's key
     * @param expireInSeconds Expire time set to the redis lock entry
     */
    public RedisReentrantLock(RedisClient redis, String key, int expireInSeconds) {
        this(redis, key, expireInSeconds, 30);
    }

    public RedisReentrantLock(RedisClient redis, String key, int expireInSeconds, int waitSeconds) {
        this.redis = redis;
        this.key = key;
        this.expireInSeconds = expireInSeconds;
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
        return tryLock(expireInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        boolean acquired = sync.tryAcquire(1);
        if (!acquired) {
            return false;
        }
        try {
            acquired = tryAcquire(time, unit);
        } catch (Exception e) {
            sync.release(1);
            return false;
        }
        if (!acquired) {
            sync.release(1);
            return false;
        }
        return true;
    }

    @Override
    public void unlock() {
        if (!sync.isHeldExclusively()) {
            throw new IllegalMonitorStateException("Current thread is not holding lock");
        }
        try {
            if (tryRelease()) {
                sync.release(1);
            }
        } catch (Exception e) {
            sync.release(1);
            throw e;
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
        if (state != 0 && sync.isHeldExclusively()) {
            state += 1;
            return;
        }
        String lock = getLock();
        while (!redis.tryAcquire(key, lock, expireInSeconds)) {
            Thread.sleep(waitSeconds);
        }
        value = lock;
        state += 1;
    }

    private boolean tryAcquire(long time, TimeUnit unit) {
        if (state != 0 && sync.isHeldExclusively()) {
            state += 1;
            return true;
        }
        String lock = getLock();
        boolean acquired = redis.tryAcquire(key, lock, (int) unit.toSeconds(time));
        if (acquired) {
            value = lock;
            state += 1;
        }
        return acquired;
    }

    private boolean tryRelease() {
        if (value == null) {
            throw new IllegalMonitorStateException();
        }
        if (!value.equals(redis.getLock(key))) {
            throw new IllegalMonitorStateException();
        }
        state -= 1;
        if (state < 0) {
            throw new IllegalMonitorStateException();
        }
        if (state > 0) {
            return false;
        }
        this.value = null;
        redis.release(key);
        return true;
    }

    private boolean hasLock() {
        return value != null;
    }

    private String getLock() {
        return hasLock() ? value : UUID.randomUUID().toString();
    }

    private class Sync extends AbstractQueuedSynchronizer {

        @Override
        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return isHeldExclusively();
        }

        @Override
        public boolean tryRelease(int acquires) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException("Current thread is not holding lock");
            }
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            int newState = getState() - acquires;
            if (newState == 0) {
                setExclusiveOwnerThread(null);
            }
            setState(newState);
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
