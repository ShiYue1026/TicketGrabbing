package com.damai.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.http.codec.ServerCodecConfigurer;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Config {

    private final AtomicInteger threadCount = new AtomicInteger(1);

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        return new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors()+10,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> new Thread(
                        Thread.currentThread().getThreadGroup(), r,
                        "listen-start-thread-" + threadCount.getAndIncrement()));
    }

    @Bean
    public ServerCodecConfigurer serverCodecConfigurer() {
        return ServerCodecConfigurer.create();
    }

}
