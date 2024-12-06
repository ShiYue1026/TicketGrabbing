package com.damai.core;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static com.damai.constant.Constant.DEFAULT_PREFIX_DISTINCTION_NAME;
import static com.damai.constant.Constant.PREFIX_DISTINCTION_NAME;

public class SpringUtil implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static ConfigurableApplicationContext configurableApplicationContext;

    public static String getPrefixDistinctionName() {
        return configurableApplicationContext.getEnvironment().getProperty(PREFIX_DISTINCTION_NAME,
                DEFAULT_PREFIX_DISTINCTION_NAME);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        configurableApplicationContext = applicationContext;
    }
}
