package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * auth: mingcheng Liu
 * date: 2025/8/3
 * desc:
 */

@Component
public class RedisIdWorker {


    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final long BEGIN_TIMESTAMP  = 1640995200L;

    private static final int COUNT_BITS  = 32;

    public long nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp =nowSecond - BEGIN_TIMESTAMP;

        //2.生成序列号
        //2.1获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.2自增长 如果key不存在，自动创建key
        long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);

        //3.拼接并返回  timestamp向左移动32位 |是或运算 有1则1
        return  timestamp<<COUNT_BITS | count;
    }
}
