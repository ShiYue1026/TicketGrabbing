-- 是否需要进行限制
local trigger_result = 0

-- 是否保存记录
local trigger_call_Stat = 0

-- 请求数
local api_count = 0

-- 规则阈值
local threshold = 0

-- 规则对象
local apiRule = cjson.decode(KEYS[1])

-- 规则类型
local api_rule_type = apiRule.apiRuleType
-- 普通规则中进行统计的时间段
local rule_stat_time = apiRule.statTime
-- 普通规则中统计的请求数
local rule_key = apiRule.ruleKey
-- 普通规则中的请求阈值
local rule_threshold = apiRule.threshold
-- 普通规则超过阈值后限制的时间
local rule_effective_time = apiRule.effectiveTime
-- 实现普通规则执行限制
local rule_limit_key = apiRule.ruleLimitKey
-- 进行统计超过普通规则的数量sorted set结构  score:当前时间 member:当前时间_请求数
local z_set_key = apiRule.zSetRuleStatKey
-- 当前时间
local current_Time = apiRule.currentTime
-- 定制的规则提示语索引
local message_index = -1

-- 请求数
local count = tonumber(redis.call('incrby', rule_key, 1))
-- 第一次的话需要设置统计的时间段（过期时间）
if(count == 1) then
    redis.call('expire', rule_key, rule_stat_time)
end
-- 如果在普通规则的统计时间下请求数超过了阈值
if(count - rule_threshold >= 0) then
    -- 如果普通规则之前没有生效限制或者限制已经失效
    if(redis.call('exists', rule_limit_key) == 0) then
        redis.call('set', rule_limit_key, rule_limit_key)
        redis.call('expire', rule_limit_key, rule_effective_time)
        -- 进行这一轮的初次限制要保存记录
        trigger_call_Stat = 1
        -- 每一轮发生初次限制保存到sorted set
        local z_set_member = current_Time .. "_" .. tostring(count)
        redis.call('zadd', z_set_key, current_Time, z_set_member)
    end
    -- 发生了限制
    trigger_result = 1
end
-- 普通规则的限制还在生效中
if(redis.call('exists', rule_limit_key) == 1) then
    trigger_result = 1
end
api_count = count
threshold = rule_threshold

-- 如果深度规则存在的话
if(api_rule_type == 2) then
    -- 获取所有的深度规则
    local depthRules = apiRule.depthRules
    -- 遍历每一个深度规则
    for index, depth_rule in ipairs(depthRules) do
        local start_time_window = depth_rule.startTimeWindowTimestamp
        local end_time_window = depth_rule.endTimeWindowTimestamp
        local depth_rule_stat_time = depth_rule.statTime
        local depth_rule_threshold = depth_rule.threshold
        local depth_rule_effective_time = depth_rule.effectiveTime
        local depth_rule_limit_key = depth_rule.depthRuleLimit

        threshold = depth_rule_threshold

        -- 清楚过期的
        if(current_Time > start_time_window) then
            redis.call('zremrangebyscore', z_set_key, 0, start_time_window)
        end
        -- 如果当前的时间在设置的时间范围内
        if(current_Time >= start_time_window and current_Time <= end_time_window) then
            local z_set_min_score = start_time_window
            local z_set_max_score = current_Time
            if((current_Time - start_time_window) > depth_rule_stat_time * 1000) then
                -- 更新开始时间范围
                z_set_min_score = current_Time - (depth_rule_stat_time * 1000)
            end
            -- 根据时间范围获取普通规则的限制数量
            local rule_trigger_count = tonumber(redis.call('zcount', z_set_key, z_set_min_score, z_set_max_score))
            api_count = rule_trigger_count
            if ((rule_trigger_count - depth_rule_threshold) >= 0) then
                if (redis.call('exists', depth_rule_limit_key) == 0) then
                    redis.call('set', depth_rule_limit_key, depth_rule_limit_key)
                    redis.call('expire', depth_rule_limit_key, depth_rule_effective_time)
                    trigger_result = 1
                    trigger_call_Stat = 2
                    message_index = index
                    return string.format('{"triggerResult": %d, "triggerCallStat": %d, "apiCount": %d, "threshold": %d, "messageIndex": %d}'
                    ,trigger_result,trigger_call_Stat,api_count,threshold,message_index)
                end
            end
            if (redis.call('exists', depth_rule_limit_key) == 1) then
                trigger_result = 1
                message_index = index
                return string.format('{"triggerResult": %d, "triggerCallStat": %d, "apiCount": %d, "threshold": %d, "messageIndex": %d}'
                ,trigger_result,trigger_call_Stat,api_count,threshold,message_index)
            end
        end
    end
end
return string.format('{"triggerResult": %d, "triggerCallStat": %d, "apiCount": %d, "threshold": %d, "messageIndex": %d}'
,trigger_result,trigger_call_Stat,api_count,threshold,message_index)