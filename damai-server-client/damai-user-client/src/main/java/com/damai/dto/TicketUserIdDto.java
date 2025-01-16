package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="TicketUserIdDto", description ="购票人id入参")
public class TicketUserIdDto {

    @Schema(name ="id", type ="Long", description ="购票人id", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

}