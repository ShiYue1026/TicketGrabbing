package com.damai.service.init;

import cn.hutool.core.collection.CollectionUtil;
import com.damai.handler.BloomFilterHandler;
import com.damai.initialize.base.AbstractApplicationPostConstructHandler;
import com.damai.service.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProgramBloomFilterInit extends AbstractApplicationPostConstructHandler {

    @Autowired
    private ProgramService programService;

    @Autowired
    private BloomFilterHandler bloomFilterHandler;

    @Override
    public Integer executeOrder() {
        return 4;
    }

    @Override
    public void executeInit(ConfigurableApplicationContext context) {
        List<Long> allProgramIdList = programService.getAllProgramIdList();
        if(CollectionUtil.isEmpty(allProgramIdList)){
            return;
        }
        allProgramIdList.forEach(programId -> bloomFilterHandler.add(String.valueOf(programId)));
    }
}
