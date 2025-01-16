package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(title="UserLogoutDto", description ="用户退出登录")
public class UserLogoutDto {

    @Schema(name ="code", type ="String", description ="渠道code 0001:pc网站", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String code;

    @Schema(name ="id", type ="Long", description ="token", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String token;
}
