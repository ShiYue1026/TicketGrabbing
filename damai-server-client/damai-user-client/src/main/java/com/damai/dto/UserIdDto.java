package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="UserIdDto", description ="用户id入参")
public class UserIdDto {

    @Schema(name ="id", type ="Long", description ="用户id", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;
}
