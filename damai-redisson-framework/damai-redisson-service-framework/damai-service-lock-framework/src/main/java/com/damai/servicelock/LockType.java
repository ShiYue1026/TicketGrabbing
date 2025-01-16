package com.damai.servicelock;

public enum LockType {
    /**
     * 可重入锁
     */
    Reentrant,

    /**
     * 公平锁
     */
    Fair,

    /**
     * 读锁
     */
    Read,

    /**
     * 写锁
     */
    Write;

    LockType() {

    }
}
