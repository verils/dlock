package com.github.verils.dlock.redis;

import com.github.verils.dlock.Toilet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedisReentrantLockToiletTest {

    private static final String TEST_LOCK_KEY = "test:lock";
    private static final int EXPIRE_IN_SECONDS = 5;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private RedisClient redis;
    private Lock lock;

    @Before
    public void setUp() {
        redis = mock(RedisClient.class);
        lock = new RedisReentrantLock(redis, TEST_LOCK_KEY, EXPIRE_IN_SECONDS);
    }

    @Test
    public void testExceptional() throws ExecutionException, InterruptedException {
        RuntimeException thrown = new RuntimeException("Test thrown when set key to redis");
        when(redis.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenThrow(thrown);

        expectedException.expect(thrown.getClass());
        expectedException.expectMessage(thrown.getMessage());

        Toilet.testReentrant(lock, 6, 20);
    }

    @Test
    public void testTryExceptional() throws ExecutionException, InterruptedException {
        RuntimeException thrown = new RuntimeException("Test thrown when set key to redis");
        when(redis.tryAcquire(eq(TEST_LOCK_KEY), anyString(), eq(EXPIRE_IN_SECONDS))).thenThrow(thrown);

        Toilet toilet = Toilet.testTry(lock, 6, 20);
        assertEquals(0, toilet.getCount());
    }
}