package com.damai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

import static com.damai.config.DelayQueueProperties.PREFIX;

@Data
@ConfigurationProperties(prefix = PREFIX)
public class DelayQueueProperties {

    public static final String PREFIX = "delay.queue";

    /**
     * 从队列拉取数据的线程池中的核心线程数量，如果业务过慢可调大
     * */
    private Integer corePoolSize = 4;
    /**
     * 从队列拉取数据的线程池中的最大线程数量，如果业务过慢可调大
     * */
    private Integer maximumPoolSize = 4;

    /**
     * 从队列拉取数据的线程池中的最大线程回收时间
     * */
    private long keepAliveTime = 30;
    /**
     * 从队列拉取数据的线程池中的最大线程回收时间的时间单位
     * */
    private TimeUnit unit = TimeUnit.SECONDS;
    /**
     * 从队列拉取数据的线程池中的队列数量，如果业务过慢可调大
     * */
    private Integer workQueueSize = 256;
}
