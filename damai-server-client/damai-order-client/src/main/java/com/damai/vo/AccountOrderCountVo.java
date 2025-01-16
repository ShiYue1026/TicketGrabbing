package com.damai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title="AccountOrderCountVo", description ="账户下订单数量")
public class AccountOrderCountVo {

    @Schema(name ="count", type ="Integer", description ="账户下的订单数量")
    private Integer count;
}

