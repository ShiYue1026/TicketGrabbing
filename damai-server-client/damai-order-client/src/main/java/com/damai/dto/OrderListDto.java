package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="OrderListDto", description ="订单列表查询")
public class OrderListDto {

    @Schema(name ="userId", type ="Long", description ="用户id", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long userId;

}
