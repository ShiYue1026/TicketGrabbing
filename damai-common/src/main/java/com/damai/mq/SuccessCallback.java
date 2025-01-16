package com.damai.mq;

@FunctionalInterface
public interface SuccessCallback<T> {

    void onSuccess(T result);

}
