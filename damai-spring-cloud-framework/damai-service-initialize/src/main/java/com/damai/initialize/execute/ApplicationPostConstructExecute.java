package com.damai.initialize.execute;

import com.damai.initialize.constant.InitializeHandlerType;
import com.damai.initialize.execute.base.AbstractApplicationExecute;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationPostConstructExecute extends AbstractApplicationExecute {

    public ApplicationPostConstructExecute(ConfigurableApplicationContext applicationContext) {
        super(applicationContext);
    }

    @PostConstruct
    public void postConstruct() {
        execute();
    }

    @Override
    public String type() {
        return InitializeHandlerType.APPLICATION_POST_CONSTRUCT;
    }
}
