package com.damai.initialize.base;

import com.damai.initialize.constant.InitializeHandlerType;

public abstract class AbstractApplicationPostConstructHandler implements InitializeHandler {

    @Override
    public String type() {
        return InitializeHandlerType.APPLICATION_POST_CONSTRUCT;
    }
}
