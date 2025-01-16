package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="AccountOrderCountDto", description ="账户下某个节目的订单数量")
public class AccountOrderCountDto {

    @Schema(name ="userId", type ="Long", description ="用户id", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long userId;

    @Schema(name ="programId", type ="Long", description ="节目id", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long programId;
}
