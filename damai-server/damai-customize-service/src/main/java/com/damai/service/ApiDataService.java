package com.damai.service;

import com.alibaba.fastjson.JSON;
import com.damai.core.RepeatExecuteLimitConstants;
import com.damai.entity.ApiData;
import com.damai.mapper.ApiDataMapper;
import com.damai.repeatexecutelimit.annotation.RepeatExecuteLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class ApiDataService {

    @Autowired
    private ApiDataMapper apiDataMapper;

    @RepeatExecuteLimit(name = RepeatExecuteLimitConstants.CONSUMER_API_DATA_MESSAGE,keys = {"#apiData.id"})
    public void saveApiData(ApiData apiData){
        ApiData dbApiData = apiDataMapper.selectById(apiData.getId());
        if (Objects.isNull(dbApiData)) {
            log.info("saveApiData apiData:{}", JSON.toJSONString(apiData));
            apiDataMapper.insert(apiData);
        }
    }
}
