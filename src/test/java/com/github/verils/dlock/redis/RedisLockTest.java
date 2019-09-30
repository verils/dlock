package com.github.verils.dlock.redis;

import com.github.verils.dlock.Toilet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedisLockTest {

    private static final String TEST_LOCK_KEY = "test:lock";
    private static final int EXPIRE_IN_SECONDS = 5;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private RedisClient redisClient;

    private Lock redisLock;

    @Before
    public void setUp() {
        redisClient = mock(RedisClient.class);
        redisLock = new RedisLock(redisClient, TEST_LOCK_KEY, EXPIRE_IN_SECONDS);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        Queue<String> queue = new ArrayBlockingQueue<>(1);
        when(redisClient.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenAnswer(invocation -> {
            queue.add(invocation.getArgument(1));
            return true;
        });
        when(redisClient.getLock(TEST_LOCK_KEY)).thenAnswer(invocation -> queue.poll());

        Toilet toilet = Toilet.test(redisLock, 6, 20);
        assertEquals(20, toilet.getCount());
    }

    @Test
    public void lockNormal() {
        when(redisClient.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenReturn(true);
        redisLock.lock();
    }

    @Test
    public void lockReentrant() {
        when(redisClient.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenReturn(true);

        expectedException.expect(IllegalMonitorStateException.class);
        expectedException.expectMessage("Cannot lock twice on a non-reentrant lock");

        redisLock.lock();
        redisLock.lock();
    }

    @Test
    public void lockExceptional() {
        RuntimeException thrown = new RuntimeException("Test thrown when set key to redis");
        when(redisClient.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenThrow(thrown);

        expectedException.expect(thrown.getClass());
        expectedException.expectMessage(thrown.getMessage());

        redisLock.lock();
    }

    @Test
    public void tryLockNormal() {
        when(redisClient.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenReturn(true);

        boolean locked = redisLock.tryLock();
        assertTrue(locked);
    }

    @Test
    public void tryLockExceptional() {
        RuntimeException thrown = new RuntimeException("Test thrown when set key to redis");
        when(redisClient.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenThrow(thrown);

        boolean locked = redisLock.tryLock();
        assertFalse(locked);
    }

    @Test
    public void unlockNormal() {
        Queue<String> queue = new ArrayBlockingQueue<>(1);
        when(redisClient.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenAnswer(invocation -> {
            queue.add(invocation.getArgument(1));
            return true;
        });
        when(redisClient.getLock(TEST_LOCK_KEY)).thenAnswer(invocation -> queue.poll());
        redisLock.lock();
        redisLock.unlock();
    }

    @Test
    public void unlockWithoutThreadLock() {
        expectedException.expect(IllegalMonitorStateException.class);
        expectedException.expectMessage("Current thread is not holding lock");

        redisLock.unlock();
    }
}
