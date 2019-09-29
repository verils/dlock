package com.github.verils.dlock;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotEquals;

public class BrokenLockTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        Toilet toilet = Toilet.test(new BrokenLock(), 8, 20);
        assertNotEquals(20, toilet.getCount());
    }
}