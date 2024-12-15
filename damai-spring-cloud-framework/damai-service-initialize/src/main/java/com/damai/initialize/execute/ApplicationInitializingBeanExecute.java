package com.damai.initialize.execute;

import com.damai.initialize.constant.InitializeHandlerType;
import com.damai.initialize.execute.base.AbstractApplicationExecute;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationInitializingBeanExecute extends AbstractApplicationExecute implements InitializingBean {

    public ApplicationInitializingBeanExecute(ConfigurableApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public void afterPropertiesSet() {
        execute();
    }

    @Override
    public String type() {
        return InitializeHandlerType.APPLICATION_INITIALIZING_BEAN;
    }

}
