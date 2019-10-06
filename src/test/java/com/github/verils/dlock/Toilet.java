package com.github.verils.dlock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

@Slf4j
public class Toilet {

    private static final String[] VISITORS = {"CCW他大爷", "CCW他二大爷", "CCW他三大爷", "CCW他四大爷"};

    private final Lock lock;

    private int count;

    private Toilet(Lock lock) {
        this.lock = lock;
    }

    private void accept(String man) {
        lock.lock();
        process(man);
    }

    private void acceptReentrant(String man) {
        lock.lock();
        lock.lock();
        process(man);
        lock.unlock();
    }

    private void tryAccept(String man) {
        boolean locked = lock.tryLock();
        if (locked) {
            process(man);
        } else {
            log.warn("Someone is using");
        }
    }

    private void process(String man) {
        // use a temp on purpose
        int temp = count;
        try {
            temp++;
            log.info("No.{} man come, name: {}", temp, man);
            Thread.sleep(100);
            log.info("No.{} man gone, name: {}", temp, man);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            count = temp;
            lock.unlock();
        }
    }

    public int getCount() {
        return count;
    }

    public static Toilet test(Lock lock, int threadCount, int executionTimes) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        Toilet toilet = new Toilet(lock);
        CompletableFuture[] futures = new CompletableFuture[executionTimes];
        try {
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

    public static Toilet testReentrant(Lock lock, int threadCount, int executionTimes) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        Toilet toilet = new Toilet(lock);
        CompletableFuture[] futures = new CompletableFuture[executionTimes];
        try {
            for (int i = 0; i < executionTimes; i++) {
                String man = VISITORS[i % 4];
                futures[i] = CompletableFuture.runAsync(() -> toilet.acceptReentrant(man), executor);
            }
            CompletableFuture<Void> future = CompletableFuture.allOf(futures);
            future.get();

            return toilet;
        } finally {
            executor.shutdown();
        }
    }

    public static Toilet testTry(Lock lock, int threadCount, int executionTimes) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        Toilet toilet = new Toilet(lock);
        CompletableFuture[] futures = new CompletableFuture[executionTimes];
        try {
            for (int i = 0; i < executionTimes; i++) {
                String man = VISITORS[i % 4];
                futures[i] = CompletableFuture.runAsync(() -> toilet.tryAccept(man), executor);
            }
            CompletableFuture<Void> future = CompletableFuture.allOf(futures);
            future.get();

            return toilet;
        } finally {
            executor.shutdown();
        }
    }
}
