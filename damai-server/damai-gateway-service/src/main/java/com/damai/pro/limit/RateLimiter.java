package com.damai.pro.limit;

import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RateLimiter {

    private final Semaphore semaphore;
    private final TimeUnit timeUnit;

    public RateLimiter(int maxPermitsPerSecond) {
        this.timeUnit = TimeUnit.SECONDS;
        this.semaphore = new Semaphore(maxPermitsPerSecond);
    }

    public void acquire() throws InterruptedException {
        if(!semaphore.tryAcquire(1, timeUnit)){
            throw new DaMaiFrameException(BaseCode.OPERATION_IS_TOO_FREQUENT_PLEASE_TRY_AGAIN_LATER);
        }
    }

    public void release(){
        semaphore.release();
    }
}
