package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="OrderCancelDto", description ="订单取消")
public class OrderCancelDto {

    @Schema(name ="orderNumber", type ="Long", description ="订单编号", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long orderNumber;

}
