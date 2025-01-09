package com.damai.config;

import com.damai.constant.LockInfoType;
import com.damai.handle.RedissonDataHandle;
import com.damai.locallock.LocalLockCache;
import com.damai.lockinfo.LockInfoHandle;
import com.damai.lockinfo.factory.LockInfoHandleFactory;
import com.damai.lockinfo.impl.RepeatExecuteLimitLockInfoHandle;
import com.damai.repeatexecutelimit.aspect.RepeatExecuteLimitAspect;
import com.damai.servicelock.factory.ServiceLockFactory;
import org.springframework.context.annotation.Bean;

public class RepeatExecuteLimitAutoConfiguration {

    @Bean(LockInfoType.REPEAT_EXECUTE_LIMIT)
    public LockInfoHandle repeatExecuteLimitHandle() {
        return new RepeatExecuteLimitLockInfoHandle();
    }

    @Bean
    public RepeatExecuteLimitAspect repeatExecuteLimitAspect(
             LocalLockCache localLockCache,
             LockInfoHandleFactory lockInfoHandleFactory,
             ServiceLockFactory serviceLockFactory,
             RedissonDataHandle redissonDataHandle
    ){
        return new RepeatExecuteLimitAspect(localLockCache, lockInfoHandleFactory,serviceLockFactory,redissonDataHandle);
    }
}
