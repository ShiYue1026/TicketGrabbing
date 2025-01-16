package com.damai.repeatexecutelimit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RepeatExecuteLimit {

    String name() default "";

    String[] keys();

    long durationTime() default 0L;

    String message() default "提交频繁，请稍后重试";

}
