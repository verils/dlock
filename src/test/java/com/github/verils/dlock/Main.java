package com.github.verils.dlock;

import com.github.verils.dlock.redis.RedisClient;
import com.github.verils.dlock.redis.RedisLock;
import com.github.verils.dlock.redis.client.JedisClient;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

public class Main {

    private static final String[] men = {"陈传伟他大爷", "陈传伟他二大爷", "陈传伟他三大爷", "陈传伟他四大爷"};

    public static void main(String[] args) {
        testRedisLock();
    }

    private static void testRedisLock() {
        JedisPool jedisPool = new JedisPool("docker.local");
        RedisClient client = new JedisClient(jedisPool);
        run(client);
    }

    private static void run(RedisClient client) {
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        Lock lock = new RedisLock(client, "toilet:lock", 5);
        Toilet toilet = new Toilet(lock);
        for (int i = 0; i < 200; i++) {
            String man = men[i % 4];
            executorService.execute(() -> toilet.usedBy(man));
        }
        executorService.shutdown();
    }
}
