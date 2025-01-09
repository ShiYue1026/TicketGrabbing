package com.damai.context;

import com.damai.core.DelayProduceQueue;

import java.util.concurrent.TimeUnit;

public class DelayQueueContext {

    private final DelayQueueBasePart delayQueueBasePart;

    public DelayQueueContext(DelayQueueBasePart delayQueueBasePart){
        this.delayQueueBasePart = delayQueueBasePart;
    }

    public void sendMessage(String topic, String content, long delayTime, TimeUnit timeUnit){
        DelayProduceQueue delayProduceQueue = new DelayProduceQueue(delayQueueBasePart.getRedissonClient(), topic);
        delayProduceQueue.offer(content, delayTime, timeUnit);
    }
}
