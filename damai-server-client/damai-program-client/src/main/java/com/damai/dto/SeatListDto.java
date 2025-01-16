package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title="SeatListDto", description ="节目座位列表")
public class SeatListDto {

    @Schema(name ="programId", type ="Long", description ="节目表id",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long programId;
}
