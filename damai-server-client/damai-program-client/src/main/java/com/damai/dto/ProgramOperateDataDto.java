package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(title="ProgramOperateDataDto", description ="节目数据操作")
public class ProgramOperateDataDto {

    @Schema(name ="programId", type ="Long", description ="节目id",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long programId;

    @Schema(name ="ticketCategoryCountMap", type ="List<TicketCategoryCountDto>",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<TicketCategoryCountDto> ticketCategoryCountDtoList;

    @Schema(name ="seatIdList", type ="List<Long>", description ="座位id集合",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<Long> seatIdList;

    @Schema(name ="sellStatus", type ="Long", description ="座位状态",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer sellStatus;
}
