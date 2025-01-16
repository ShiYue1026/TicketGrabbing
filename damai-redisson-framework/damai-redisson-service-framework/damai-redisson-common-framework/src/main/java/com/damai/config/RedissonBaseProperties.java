package com.damai.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

@Data
@ConfigurationProperties(prefix = "spring.redis.redisson")
public class RedissonBaseProperties {

    private Integer threads = 16;

    private Integer nettyThreads = 32;

    private Integer corePoolSize = null;

    private Integer maximumPoolSize = null;

    private long keepAliveTime = 30;

    private TimeUnit unit = TimeUnit.SECONDS;

    private Integer workQueueSize = 256;

}
