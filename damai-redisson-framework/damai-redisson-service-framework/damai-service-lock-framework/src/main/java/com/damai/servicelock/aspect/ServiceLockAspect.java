package com.damai.servicelock.aspect;

import com.damai.constant.LockInfoType;
import com.damai.lockinfo.LockInfoHandle;
import com.damai.lockinfo.factory.LockInfoHandleFactory;
import com.damai.servicelock.LockType;
import com.damai.servicelock.ServiceLocker;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.servicelock.factory.ServiceLockFactory;
import com.damai.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Order(-10)
@AllArgsConstructor
public class ServiceLockAspect {

    private final LockInfoHandleFactory lockInfoHandleFactory;

    private final ServiceLockFactory serviceLockFactory;

    @Around("@annotation(serviceLock)")
    public Object around(ProceedingJoinPoint joinPoint, ServiceLock serviceLock) throws Throwable {
        log.info("分布式锁判断：{}", joinPoint.getSignature().getName());
        LockInfoHandle lockInfoHandle = lockInfoHandleFactory.getLockInfoHandle(LockInfoType.SERVICE_LOCK);
        String lockName = lockInfoHandle.getLockName(joinPoint, serviceLock.name(), serviceLock.keys());
        LockType lockType = serviceLock.lockType();
        long waitTime = serviceLock.waitTime();
        TimeUnit timeUnit = serviceLock.timeUnit();

        ServiceLocker lock = serviceLockFactory.getLock(lockType);
        boolean result = lock.tryLock(lockName, timeUnit, waitTime);

        if(result) {
            try {
                return joinPoint.proceed();
            } finally {
                lock.unlock(lockName);
            }
        } else{
            log.warn("Timeout while acquiring serviceLock:{}",lockName);
            String customLockTimeoutStrategy = serviceLock.customLockTimeoutStrategy();
            if (StringUtil.isNotEmpty(customLockTimeoutStrategy)) {
                return handleCustomLockTimeoutStrategy(customLockTimeoutStrategy, joinPoint);
            }else{
                serviceLock.lockTimeoutStrategy().handler(lockName);
            }
            return joinPoint.proceed();
        }
    }

    private Object handleCustomLockTimeoutStrategy(String customLockTimeoutStrategy, ProceedingJoinPoint joinPoint) {
        Method currentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object target = joinPoint.getTarget();
        Method handleMethod = null;

        try{
            handleMethod = target.getClass().getDeclaredMethod(customLockTimeoutStrategy);
            handleMethod.setAccessible(true);
        } catch(NoSuchMethodException e){
            throw new RuntimeException("Illegal annotation param customLockTimeoutStrategy :" + customLockTimeoutStrategy,e);
        }

        Object[] args = joinPoint.getArgs();
        // invoke
        Object result;
        try {
            result = handleMethod.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Fail to illegal access custom lock timeout handler: " + customLockTimeoutStrategy ,e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Fail to invoke custom lock timeout handler: " + customLockTimeoutStrategy ,e);
        }
        return result;
    }
}
