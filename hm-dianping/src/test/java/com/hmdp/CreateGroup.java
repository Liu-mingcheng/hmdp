package com.hmdp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * auth: mingcheng Liu
 * date: 2025/8/8
 * desc:
 */
@SpringBootTest
public class CreateGroup {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void createGroup(){
        DefaultRedisScript<Long> createGroup_SCRIPT = new DefaultRedisScript<>();
        createGroup_SCRIPT.setLocation(new ClassPathResource("createGroup.lua"));
        createGroup_SCRIPT.setResultType(Long.class);
        stringRedisTemplate.execute(createGroup_SCRIPT, Collections.emptyList());
    }
}
