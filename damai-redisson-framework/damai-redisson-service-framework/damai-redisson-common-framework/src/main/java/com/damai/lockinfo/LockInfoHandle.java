package com.damai.lockinfo;

import org.aspectj.lang.JoinPoint;

public interface LockInfoHandle {

    /**
     * 获取锁信息
     * @param joinPoint 切面
     * @param name 锁业务名
     * @param keys 锁
     * @return 锁信息
     * */
    String getLockName(JoinPoint joinPoint, String name, String[] keys);

    /**
     * 拼装锁信息
     * @param name 锁业务名
     * @param keys 锁
     * @return 锁信息
     * */
    String simpleGetLockName(String name, String[] keys);
}
