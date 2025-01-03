package com.damai.service.init;

import com.damai.BusinessThreadPool;
import com.damai.initialize.base.AbstractApplicationStartEventListenerHandler;
import com.damai.service.ProgramCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ProgramCategoryInitData extends AbstractApplicationStartEventListenerHandler {

    @Autowired
    private ProgramCategoryService programCategoryService;


    @Override
    public Integer executeOrder() {
        return 1;
    }

    @Override
    public void executeInit(ConfigurableApplicationContext context) {
        BusinessThreadPool.execute(() -> {
            programCategoryService.programCategoryRedisDataInit();
        });
    }
}
