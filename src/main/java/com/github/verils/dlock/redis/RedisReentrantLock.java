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
            reset();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            reset();
            throw e;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
        try {
            acquire();
        } catch (Exception e) {
            reset();
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
            release();
        } catch (Exception e) {
            sync.release(1);
            throw e;
        }
        if (state == 0) {
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
        boolean reentrant = isHeldExclusively();
        if (reentrant) {
            state += 1;
            redis.expire(key, expireInSeconds);
            return;
        }
        String lock = newLock();
        while (!redis.tryAcquire(key, lock, expireInSeconds)) {
            Thread.sleep(waitSeconds);
        }
        value = lock;
        state += 1;
    }

    private boolean tryAcquire(long time, TimeUnit unit) {
        String lock = newLock();
        boolean acquired = redis.tryAcquire(key, lock, (int) unit.toSeconds(time));
        if (acquired) {
            value = lock;
            state += 1;
            return true;
        }
        if (isHeldExclusively()) {
            state += 1;
            redis.expire(key, (int) unit.toSeconds(time));
            return true;
        }
        return false;
    }

    private void reset() {
        value = null;
        state = 0;
        sync.release(1);
    }

    private void release() {
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
            return;
        }
        this.value = null;
        redis.release(key);
    }

    private boolean isHeldExclusively() {
        return value != null && value.equals(redis.getLock(key));
    }

    private String newLock() {
        return UUID.randomUUID().toString();
    }

    private static class Sync extends AbstractQueuedSynchronizer {

        @Override
        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                if (log.isDebugEnabled()) {
                    log.debug("Acquired thread synchronizer for: 1");
                }
                return true;
            }
            boolean reentrant = isHeldExclusively();
            if (reentrant) {
                if (log.isDebugEnabled()) {
                    log.debug("Acquired thread synchronizer for: 1");
                }
            }
            return reentrant;
        }

        @Override
        public boolean tryRelease(int acquires) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException("Current thread is not holding lock");
            }
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            if (log.isDebugEnabled()) {
                log.debug("Released thread synchronizer for: 1");
            }
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
