package com.damai.event;

import cn.hutool.core.collection.CollectionUtil;
import com.damai.context.DelayQueueBasePart;
import com.damai.context.DelayQueuePart;
import com.damai.core.ConsumerTask;
import com.damai.core.DelayConsumerQueue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class DelayQueueInitHandler  implements ApplicationListener<ApplicationStartedEvent> {

    private final DelayQueueBasePart delayQueueBasePart;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        log.info("延迟消费队列初始化...");

        Map<String, ConsumerTask> consumerTaskMap = event.getApplicationContext().getBeansOfType(ConsumerTask.class);
        if (CollectionUtil.isEmpty(consumerTaskMap)) {
            return;
        }

        for (ConsumerTask consumerTask : consumerTaskMap.values()) {
            log.info("消费队列：{}", consumerTask.topic());
            DelayQueuePart delayQueuePart = new DelayQueuePart(delayQueueBasePart, consumerTask);
            DelayConsumerQueue delayConsumerQueue = new DelayConsumerQueue(delayQueuePart, delayQueuePart.getConsumerTask().topic());
            delayConsumerQueue.listenStart();
        }
    }
}
