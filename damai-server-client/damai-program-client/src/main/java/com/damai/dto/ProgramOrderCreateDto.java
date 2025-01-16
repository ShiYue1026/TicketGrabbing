package com.damai.dto;

import com.damai.data.BaseTableData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(title="ProgramOrderCreateDto", description ="节目订单创建")
public class ProgramOrderCreateDto {

    @Schema(name ="programId", type ="Long", description ="节目id",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long programId;

    @Schema(name ="userId", type ="Long", description ="用户id",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long userId;

    @Schema(name ="ticketUserIdList", type ="List<Long>", description ="购票人id集合",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<Long> ticketUserIdList;

    @Schema(name ="seatDtoList", type ="List<SeatDto>", description = "座位")
    private List<SeatDto> seatDtoList;

    @Schema(name ="ticketCategoryId", type ="Long", description = "节目票档id(如果不选座位，那么票档id必填)")
    private Long ticketCategoryId;

    @Schema(name ="ticketCount", type ="Integer", description = "购买票数量(如果不选座位，那么购买票数量必填)")
    private Integer ticketCount;
}

