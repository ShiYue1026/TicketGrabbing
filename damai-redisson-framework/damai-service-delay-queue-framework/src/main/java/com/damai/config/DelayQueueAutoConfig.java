package com.damai.config;

import com.damai.context.DelayQueueBasePart;
import com.damai.context.DelayQueueContext;
import com.damai.event.DelayQueueInitHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(DelayQueueProperties.class)
public class DelayQueueAutoConfig {

    @Bean
    public DelayQueueInitHandler delayQueueInitHandler(DelayQueueBasePart delayQueueBasePart){
        return new DelayQueueInitHandler(delayQueueBasePart);
    }

    @Bean
    public DelayQueueBasePart delayQueueBasePart(RedissonClient redissonClient, DelayQueueProperties delayQueueProperties){
        return new DelayQueueBasePart(redissonClient,delayQueueProperties);
    }

    @Bean
    public DelayQueueContext delayQueueContext(DelayQueueBasePart delayQueueBasePart){
        return new DelayQueueContext(delayQueueBasePart);
    }

}
