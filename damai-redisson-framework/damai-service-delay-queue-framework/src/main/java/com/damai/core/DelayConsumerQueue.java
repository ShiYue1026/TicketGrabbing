package com.damai.core;

import com.damai.config.DelayQueueProperties;
import com.damai.context.DelayQueueBasePart;
import com.damai.context.DelayQueuePart;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DelayConsumerQueue extends DelayBaseQueue {

    private final AtomicInteger listenStartThreadCount = new AtomicInteger(1);

    private final AtomicInteger executeTaskThreadCount = new AtomicInteger(1);

    private final ThreadPoolExecutor listenStartThreadPool;

    private final ThreadPoolExecutor executeTaskThreadPool;

    private final ConsumerTask consumerTask;

    private final AtomicBoolean runFlag = new AtomicBoolean(false);

    public DelayConsumerQueue(DelayQueuePart delayQueuePart, String relTopic) {
        super(delayQueuePart.getDelayQueueBasePart().getRedissonClient(), relTopic);
        this.listenStartThreadPool = new ThreadPoolExecutor(
                1, 1, 60,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                        "listen-start-thread-" + listenStartThreadCount.getAndIncrement()) );
        this.executeTaskThreadPool = new ThreadPoolExecutor(
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getCorePoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getMaximumPoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getKeepAliveTime(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getUnit(),
                new LinkedBlockingQueue<>(delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getWorkQueueSize()),
                r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                        "delay-queue-consume-thread-" + executeTaskThreadCount.getAndIncrement()));
        this.consumerTask = delayQueuePart.getConsumerTask();
    }

    public void listenStart(){
        if(!runFlag.get()){
            log.info("消费延迟队列开始监听......");
            runFlag.set(true);
            listenStartThreadPool.execute(() -> {
                while(!Thread.interrupted()){
                    try{
                        assert blockingQueue != null;
                        String content = blockingQueue.take();
                        executeTaskThreadPool.execute(() -> {
                            try{
                                consumerTask.execute(content);
                            } catch(Exception e){
                                log.error("consumer execute error", e);
                            }
                        });
                    } catch (InterruptedException e){
                        destroy(executeTaskThreadPool);
                    } catch (Throwable e) {
                        log.error("blockingQueue take error",e);
                    }
                }
            });
        }
    }

    public void destroy(ExecutorService executorService) {
        try {
            if (Objects.nonNull(executorService)) {
                executorService.shutdown();
            }
        } catch (Exception e) {
            log.error("destroy error",e);
        }
    }
}
