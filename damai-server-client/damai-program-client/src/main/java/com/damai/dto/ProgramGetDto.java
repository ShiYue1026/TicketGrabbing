package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="ProgramGetDto", description ="节目")
public class ProgramGetDto{

    @Schema(name ="id", type ="Long", description ="id",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;
}
