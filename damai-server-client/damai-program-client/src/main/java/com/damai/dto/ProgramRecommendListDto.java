package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title="ProgramRecommendListDto", description ="节目推荐列表")
public class ProgramRecommendListDto {

    @Schema(name ="areaId", type ="Long", description ="所在区域id")
    private Long areaId;

    @Schema(name ="parentProgramCategoryId", type ="Long", description ="父节目类型id")
    private Long parentProgramCategoryId;

    @Schema(name ="programId", type ="Long", description ="查看节目详情时，调用推荐列表时要传入此节目id")
    private Long programId;
}
