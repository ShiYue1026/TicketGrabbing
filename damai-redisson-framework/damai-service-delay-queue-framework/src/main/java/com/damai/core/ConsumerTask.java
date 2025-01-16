package com.damai.core;

public interface ConsumerTask {

    void execute(String content);

    String topic();

}
