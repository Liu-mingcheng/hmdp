package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.aspectj.weaver.ast.Var;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * auth: mingcheng Liu
 * date: 2025/8/6
 * desc:
 */
public class SimpleRedisLock implements ILock{

    private String name;

    private static  final String KEY_PREFIX = "lock:";

    private static  final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {

        //获取线程标示
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX+name, threadId, timeoutSec, TimeUnit.SECONDS);

        //避免空指针风险 True返回ture，null返回false
        return Boolean.TRUE.equals(success);
    }

    /*@Override
    public void unlock() {
        //获取线程标示
        String threadId =ID_PREFIX+ Thread.currentThread().getId();

        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);

        //判断标示是否一致
        if(threadId.equals(id)){
            stringRedisTemplate.delete(KEY_PREFIX+name);
        }

    }*/

    @Override
    public void unlock() {
        //调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX+ Thread.currentThread().getId());

    }


}
