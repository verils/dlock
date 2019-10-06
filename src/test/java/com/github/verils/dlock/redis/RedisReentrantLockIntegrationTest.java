package com.github.verils.dlock.redis;

import com.github.verils.dlock.Toilet;
import com.github.verils.dlock.redis.client.JedisClient;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.assertEquals;

public class RedisReentrantLockIntegrationTest {

    private Lock lock;

    @Before
    public void setUp() {
        JedisPool jedisPool = new JedisPool("docker.local");
        RedisClient client = new JedisClient(jedisPool);
        lock = new RedisReentrantLock(client, "toilet:lock", 5);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        Toilet toilet = Toilet.testReentrant(lock, 6, 20);
        assertEquals(20, toilet.getCount());
    }

    @Test
    public void testTry() throws ExecutionException, InterruptedException {
        Toilet toilet = Toilet.testTry(lock, 6, 20);
        assertEquals(1, toilet.getCount());
    }
}
