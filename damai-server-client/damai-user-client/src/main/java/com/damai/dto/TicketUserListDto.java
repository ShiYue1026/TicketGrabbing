package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="TicketUserListDto", description ="购票人列表入参")
public class TicketUserListDto {

    @Schema(name ="userId", type ="Long", description ="用户id", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long userId;
}

