package com.todaydev.common.config;

import com.todaydev.common.config.properties.RedisKeyProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisKeyConfig {

    @Bean
    public RedisKeyFactory redisKeyFactory(RedisKeyProperties properties) {
        return new RedisKeyFactory(properties);
    }
}
