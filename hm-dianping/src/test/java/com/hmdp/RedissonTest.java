package com.hmdp;

import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * auth: mingcheng Liu
 * date: 2025/8/7
 * desc:
 */

@SpringBootTest
@Slf4j
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedissonClient redissonClient2;

    private RLock lock;

    @BeforeEach
    void setup(){
        RLock lock1 = redissonClient.getLock("order");
        RLock lock2 = redissonClient2.getLock("order");

        //创建联锁multiLock
        lock =redissonClient .getMultiLock(lock1,lock2);
    }

    @Test
    void method1() throws InterruptedException {
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
        if(!isLock){
            log.error("获取锁失败,1");
            return;
        }
        try {
            log.info("获取锁成功,1");
            method2();
        }finally {
            log.info("释放锁,1");
            lock.unlock();
        }
    }

    @Test
    void method2() throws InterruptedException {
        boolean isLock = lock.tryLock();
        if(!isLock){
            log.error("获取锁失败,2");
            return;
        }
        try {
            log.info("获取锁成功,2");
        }finally {
            log.info("释放锁,2");
            lock.unlock();
        }
    }
}
