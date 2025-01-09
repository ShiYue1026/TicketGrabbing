package com.damai.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;

@Slf4j
public class DelayBaseQueue {

    protected final RedissonClient redissonClient;

    protected final RBlockingQueue<String> blockingQueue;

    public DelayBaseQueue(RedissonClient redissonClient, String relTopic) {
        this.redissonClient = redissonClient;
        this.blockingQueue = redissonClient.getBlockingQueue(relTopic);
    }

}
