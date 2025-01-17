package com.damai.vo;

import lombok.Data;

import java.util.Date;

@Data
public class DepthRuleVo {

    private String id;

    private String startTimeWindow;

    private long startTimeWindowTimestamp;

    private String endTimeWindow;

    private long endTimeWindowTimestamp;

    private Integer statTime;

    private Integer statTimeType;

    private Integer threshold;

    private Integer effectiveTime;

    private Integer effectiveTimeType;

    private String limitApi;

    private String message;

    private Integer status;

    private Date createTime;
}
