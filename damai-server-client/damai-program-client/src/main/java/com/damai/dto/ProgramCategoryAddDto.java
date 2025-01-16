package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(title="ProgramCategoryAddDto", description ="节目类型")
public class ProgramCategoryAddDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 父区域id
     */
    @Schema(name ="parentId", type ="Long", description ="父区域id",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long parentId;

    /**
     * 区域名字
     */
    @Schema(name ="name", type ="String", description ="区域名字",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    /**
     * 1:一级种类 2:二级种类
     */
    @Schema(name ="type", type ="Integer", description ="1:一级种类 2:二级种类",requiredMode= Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer type;
}