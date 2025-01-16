package com.damai.servicelock.annotation;

import com.damai.servicelock.LockType;
import com.damai.servicelock.info.LockTimeOutStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ServiceLock {

    LockType lockType() default LockType.Reentrant;

    String name() default "";

    String[] keys();

    long waitTime() default 10;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    LockTimeOutStrategy lockTimeoutStrategy() default LockTimeOutStrategy.FAIL;

    String customLockTimeoutStrategy() default "";
}
