package com.damai.initialize.base;

import com.damai.initialize.constant.InitializeHandlerType;

public abstract class AbstractApplicationInitializingBeanHandler implements InitializeHandler {

    @Override
    public String type() {
        return InitializeHandlerType.APPLICATION_INITIALIZING_BEAN;
    }
}
