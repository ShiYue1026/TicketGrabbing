package com.damai.service.delayconsumer;

import com.alibaba.fastjson2.JSON;
import com.damai.core.ConsumerTask;
import com.damai.core.SpringUtil;
import com.damai.dto.ProgramOperateDataDto;
import com.damai.service.ProgramService;
import com.damai.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.damai.service.constant.ProgramOrderConstant.DELAY_OPERATE_PROGRAM_DATA_TOPIC;

@Slf4j
@Component
public class
DelayOperateProgramDataConsumer implements ConsumerTask {

    @Autowired
    private ProgramService programService;

    @Override
    public void execute(String content) {
        log.info("延迟操作节目数据消息进行消费 content : {}", content);
        if (StringUtil.isEmpty(content)){
            log.error("延迟队列消息不存在");
            return;
        }
        ProgramOperateDataDto programOperateDataDto = JSON.parseObject(content, ProgramOperateDataDto.class);
        programService.operateProgramData(programOperateDataDto);
    }

    @Override
    public String topic() {
        return SpringUtil.getPrefixDistinctionName() + "-" + DELAY_OPERATE_PROGRAM_DATA_TOPIC;
    }
}
