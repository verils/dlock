package com.github.verils.example.dlock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;

@Slf4j
public class Toilet {

    private final Lock lock;

    public Toilet(Lock lock) {
        this.lock = lock;
    }

    public void usedBy(String man) {
        lock.lock();
//        boolean locked = lock.tryLock();
//        if (locked) {
            try {
//                lock.lock();
                log.info("{} 来了", man);
                Thread.sleep(600);
                log.info("{} 走了", man);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
//        } else {
//            log.warn("Can't acquire lock");
//        }
    }

}
