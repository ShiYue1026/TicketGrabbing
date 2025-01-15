package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(title="OrderPayDto", description ="订单支付")
public class OrderPayDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(name ="platform", type ="Integer", description ="支付平台 1：小程序  2：H5  3：pc网页  4：app", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer platform;

    @Schema(name ="orderNumber", type ="Long", description ="订单编号", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long orderNumber;

    @Schema(name ="subject", type ="String", description ="订单标题", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String subject;

    @Schema(name ="price", type ="BigDecimal", description ="价格", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private BigDecimal price;

    @Schema(name ="channel", type ="Integer", description ="支付渠道 alipay：支付宝 wx：微信", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String channel;

    @Schema(name ="payBillType", type ="Integer", description ="支付种类 1节目",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer payBillType;
}
