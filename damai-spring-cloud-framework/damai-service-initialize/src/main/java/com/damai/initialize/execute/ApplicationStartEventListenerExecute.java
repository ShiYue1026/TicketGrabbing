package com.damai.initialize.execute;

import com.damai.initialize.constant.InitializeHandlerType;
import com.damai.initialize.execute.base.AbstractApplicationExecute;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationStartEventListenerExecute extends AbstractApplicationExecute implements ApplicationListener<ApplicationStartedEvent> {

    public ApplicationStartEventListenerExecute(ConfigurableApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        execute();
    }

    @Override
    public String type() {
        return InitializeHandlerType.APPLICATION_EVENT_LISTENER;
    }
}
