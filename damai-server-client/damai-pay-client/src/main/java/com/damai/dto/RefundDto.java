package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(title="RefundDto", description ="退款")
public class RefundDto {

    @Schema(name ="orderNumber", type ="Long", description ="订单号",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String orderNumber;

    @Schema(name ="amount", type ="BigDecimal", description ="退款金额",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private BigDecimal amount;

    @Schema(name ="channel", type ="Integer", description ="退款渠道 alipay：支付宝 wx：微信",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private String channel;

    @Schema(name ="reason", type ="String", description ="退款原因")
    private String reason;
}

