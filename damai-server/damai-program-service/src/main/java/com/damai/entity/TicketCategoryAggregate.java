package com.damai.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TicketCategoryAggregate implements Serializable {

    /**
     * 节目表id
     */
    private Long programId;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;
}