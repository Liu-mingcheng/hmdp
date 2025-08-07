package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * auth: mingcheng Liu
 * date: 2025/8/7
 * desc:
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        //添加redis地址，这里添加了单点的地址，也可以使用useClusterServers()添加集群地址
        config.useSingleServer().setAddress("redis://127.0.0.1:6380");
        //创建客户端
        return Redisson.create(config);
    }

    @Bean
    public RedissonClient redissonClient2(){
        Config config = new Config();
        //添加redis地址，这里添加了单点的地址，也可以使用useClusterServers()添加集群地址
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        //创建客户端
        return Redisson.create(config);
    }
}
