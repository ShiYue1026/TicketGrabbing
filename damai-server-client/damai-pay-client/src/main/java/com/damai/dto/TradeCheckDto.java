package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title="TradeCheckDto", description ="交易状态入参")
public class TradeCheckDto implements Serializable {

    @Schema(name ="outTradeNo", type ="String", description ="商户订单号", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String outTradeNo;

    @Schema(name ="channel", type ="Integer", description ="支付渠道 alipay：支付宝 wx：微信",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String channel;
}
