package com.github.verils.dlock.redis;

import com.github.verils.dlock.Toilet;
import com.github.verils.dlock.redis.client.JedisClient;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

public class RedisLockIntegrationTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        JedisPool jedisPool = new JedisPool("docker.local");
        RedisClient client = new JedisClient(jedisPool);
        Lock lock = new RedisLock(client, "toilet:lock", 5);
        Toilet.test(lock, 6, 20);
    }
}
