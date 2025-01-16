package com.damai.config;

import com.damai.handler.BloomFilterHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(BloomFilterProperties.class)
public class BloomFilterAutoConfiguration {

    @Bean
    public BloomFilterHandler rBloomFilterUtil(RedissonClient redissonClient, BloomFilterProperties bloomFilterProperties){
        return new BloomFilterHandler(redissonClient, bloomFilterProperties);
    }
}
