package com.damai.initialize.config;

import com.damai.initialize.execute.ApplicationCommandLineRunnerExecute;
import com.damai.initialize.execute.ApplicationInitializingBeanExecute;
import com.damai.initialize.execute.ApplicationPostConstructExecute;
import com.damai.initialize.execute.ApplicationStartEventListenerExecute;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

public class InitializeAutoConfig {

    @Bean
    public ApplicationCommandLineRunnerExecute applicationCommandLineRunnerExecute(ConfigurableApplicationContext applicationContext){
        return new ApplicationCommandLineRunnerExecute(applicationContext);
    }

    @Bean
    public ApplicationInitializingBeanExecute applicationInitializingBeanExecute(ConfigurableApplicationContext applicationContext){
        return new ApplicationInitializingBeanExecute(applicationContext);
    }

    @Bean
    public ApplicationPostConstructExecute applicationPostConstructExecute(ConfigurableApplicationContext applicationContext){
        return new ApplicationPostConstructExecute(applicationContext);
    }

    @Bean
    public ApplicationStartEventListenerExecute applicationStartEventListenerExecute(ConfigurableApplicationContext applicationContext){
        return new ApplicationStartEventListenerExecute(applicationContext);
    }
}
