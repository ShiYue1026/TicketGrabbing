package com.damai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(title="ProgramSimpleInfoVo", description ="节目简单信息")
public class ProgramSimpleInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name ="id", type ="Long", description ="主键id")
    private Long programId;

    @Schema(name ="areaId", type ="Long", description ="地区id")
    private Long areaId;

    @Schema(name ="areaIdName", type ="String", description ="地区名字")
    private String areaIdName;
}

