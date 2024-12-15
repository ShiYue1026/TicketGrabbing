package com.damai.initialize.execute;

import com.damai.initialize.constant.InitializeHandlerType;
import com.damai.initialize.execute.base.AbstractApplicationExecute;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationCommandLineRunnerExecute extends AbstractApplicationExecute implements CommandLineRunner {

    public ApplicationCommandLineRunnerExecute(ConfigurableApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public void run(String... args) {
        execute();
    }

    @Override
    public String type() {
        return InitializeHandlerType.APPLICATION_COMMAND_LINE_RUNNER;
    }
}
