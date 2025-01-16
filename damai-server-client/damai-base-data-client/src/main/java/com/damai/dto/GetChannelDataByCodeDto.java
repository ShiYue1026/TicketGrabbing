package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@Schema(title="GetChannelDataByCodeDto", description = "渠道数据查询")
public class GetChannelDataByCodeDto {

    @Schema(name = "code", type = "String", description = "code码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String code;
}
