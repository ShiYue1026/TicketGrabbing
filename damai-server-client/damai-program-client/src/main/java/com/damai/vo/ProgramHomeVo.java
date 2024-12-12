package com.damai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(title="ProgramHomeVo", description ="节目主页列表")
public class ProgramHomeVo implements Serializable  {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name ="categoryName", type ="String", description ="类型名字")
    private String categoryName;

    @Schema(name ="categoryId", type ="Long", description ="类型id")
    private Long categoryId;

    @Schema(name ="programListVoList", type ="array", description ="节目列表")
    private List<ProgramListVo> programListVoList;
}

