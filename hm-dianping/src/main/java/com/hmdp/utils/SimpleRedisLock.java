package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.aspectj.weaver.ast.Var;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * auth: mingcheng Liu
 * date: 2025/8/6
 * desc:
 */
public class SimpleRedisLock implements ILock{

    private String name;

    private static  final String KEY_PREFIX = "lock:";

    private StringRedisTemplate stringRedisTemplate;


    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {

        //获取线程提示
        long threadId = Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX+name, threadId+"", timeoutSec, TimeUnit.SECONDS);

        //避免空指针风险 True返回ture，null返回false
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.delete(KEY_PREFIX+name);
    }
}
