package com.damai.lock;

@FunctionalInterface
public interface LockTask<V> {

    V execute();

}
