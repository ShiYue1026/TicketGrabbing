package com.damai.mq;

@FunctionalInterface
public interface FailureCallback {

    void onFailure(Throwable ex);

}
