package com.github.verils.dlock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.assertEquals;

public class MutexTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Lock mutex;

    @Before
    public void setUp() {
        mutex = new Mutex();
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        Toilet toilet = Toilet.test(mutex, 6, 20);
        assertEquals(20, toilet.getCount());
    }

    @Test
    public void unlockWithoutThreadLock() {
        expectedException.expect(IllegalMonitorStateException.class);
        expectedException.expectMessage("Current thread is not holding lock");

        mutex.unlock();
    }
}