package com.github.verils.dlock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

@Slf4j
public class Toilet {

    public static final String[] VISITORS = {"CCW他大爷", "CCW他二大爷", "CCW他三大爷", "CCW他四大爷"};

    private final Lock lock;

    private int count;

    public Toilet(Lock lock) {
        this.lock = lock;
    }

    public void accept(String man) {
        lock.lock();
        count++;
//        boolean locked = lock.tryLock();
//        if (locked) {
        try {
//                lock.lock();
            log.info("{}来了, count: {}", man, count);
            Thread.sleep(100);
            log.info("{}走了, count: {}", man, count);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
//        } else {
//            log.warn("Can't acquire lock");
//        }
    }

    public int getCount() {
        return count;
    }

    public static Toilet test(Lock lock, int threadCount, int executionTimes) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            Toilet toilet = new Toilet(lock);

            CompletableFuture[] futures = new CompletableFuture[executionTimes];
            for (int i = 0; i < executionTimes; i++) {
                String man = VISITORS[i % 4];
                futures[i] = CompletableFuture.runAsync(() -> toilet.accept(man), executor);
            }
            CompletableFuture<Void> future = CompletableFuture.allOf(futures);
            future.get();

            return toilet;
        } finally {
            executor.shutdown();
        }
    }
}
