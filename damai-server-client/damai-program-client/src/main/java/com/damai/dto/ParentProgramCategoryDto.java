package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="ParentProgramCategoryDto", description ="父节目类型")
public class ParentProgramCategoryDto {

    @Schema(name ="parentProgramCategoryId", requiredMode= Schema.RequiredMode.REQUIRED, type ="Long", description ="父节目类型id")
    @NotNull
    private Long parentProgramCategoryId;
}

