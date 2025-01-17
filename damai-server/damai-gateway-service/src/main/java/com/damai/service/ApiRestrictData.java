package com.damai.service;

import lombok.Data;

@Data
public class ApiRestrictData {

    // 是否需要进行限制
    private Long triggerResult;

    // 是否进行保存记录
    private Long triggerCallStat;

    // 请求数
    private Long apiCount;

    // 规则阈值
    private Long threshold;

    // 规则提示语索引
    private Long messageIndex;

}
