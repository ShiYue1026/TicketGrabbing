package com.damai.config;

import com.damai.constant.LockInfoType;
import com.damai.core.ManageLocker;
import com.damai.lockinfo.LockInfoHandle;
import com.damai.lockinfo.factory.LockInfoHandleFactory;
import com.damai.lockinfo.impl.ServiceLockInfoHandle;
import com.damai.servicelock.aspect.ServiceLockAspect;
import com.damai.servicelock.factory.ServiceLockFactory;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;

public class ServiceLockAutoConfiguration {

    @Bean(LockInfoType.SERVICE_LOCK)
    public LockInfoHandle serviceLockInfoHandle() {
        return new ServiceLockInfoHandle();
    }

    @Bean
    public ManageLocker manageLocker(RedissonClient redissonClient) {
        return new ManageLocker(redissonClient);
    }

    @Bean
    public ServiceLockFactory serviceLockFactory(ManageLocker manageLocker) {
        return new ServiceLockFactory(manageLocker);
    }

    @Bean
    public ServiceLockAspect serviceLockAspect(LockInfoHandleFactory lockInfoHandleFactory, ServiceLockFactory serviceLockFactory){
        return new ServiceLockAspect(lockInfoHandleFactory, serviceLockFactory);
    }


}
