package com.damai.initialize.base;

import com.damai.initialize.constant.InitializeHandlerType;

public abstract class AbstractApplicationStartEventListenerHandler implements InitializeHandler{

    @Override
    public String type(){
        return InitializeHandlerType.APPLICATION_EVENT_LISTENER;
    }
}
