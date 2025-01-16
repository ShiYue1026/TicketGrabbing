package com.damai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(title="UserLoginDto", description ="用户登录")
public class UserLoginDto {

    @Schema(name ="code", type ="String", description ="渠道code 0001:pc网站", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String code;

    @Schema(name ="mobile", type ="String", description ="用户手机号")
    private String mobile;

    @Schema(name ="email", type ="String", description ="用户邮箱")
    private String email;

    @Schema(name ="password", type ="String", description ="密码", requiredMode= Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String password;
}