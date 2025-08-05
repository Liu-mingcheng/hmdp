package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * auth: mingcheng Liu
 * date: 2025/7/31
 * desc:
 */

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //登录拦截器
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/user/login","/user/code",
                "/blog/hot",
                "/shop/**","/shop-type/**",
                "/upload/**",
                "/voucher/**"
        ).order(1);
        //token刷新拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
