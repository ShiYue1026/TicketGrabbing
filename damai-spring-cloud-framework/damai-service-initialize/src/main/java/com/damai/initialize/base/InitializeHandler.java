package com.damai.initialize.base;

import org.springframework.context.ConfigurableApplicationContext;

public interface InitializeHandler {

    /**
     * 初始化执行 类型
     * @return 类型
     * */
    String type();

    /**
     * 同一个类型下的初始化执行顺序
     * @return 顺序
     * */
    Integer executeOrder();

    /**
     * 执行逻辑
     * @param context 容器上下文
     * */
    void executeInit(ConfigurableApplicationContext context);

}
