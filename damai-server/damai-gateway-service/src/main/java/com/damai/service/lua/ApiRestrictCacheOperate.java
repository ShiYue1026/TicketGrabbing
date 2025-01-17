package com.damai.service.lua;

import com.alibaba.fastjson2.JSON;
import com.damai.redis.RedisCache;
import com.damai.service.ApiRestrictData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ApiRestrictCacheOperate {

    @Autowired
    private RedisCache redisCache;

    private DefaultRedisScript<String> redisScript;

    @PostConstruct
    public void init() {
        try{
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/apiLimit.lua")));
        } catch (Exception e){
            log.error("redisScript init lua error", e);
        }
    }

    public ApiRestrictData apiRuleOperate(List<String> keys, Object[] args) {
        Object object = redisCache.getInstance().execute(redisScript, keys, args);
        return JSON.parseObject((String) object, ApiRestrictData.class);
    }
}
