package com.damai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title="CheckNeedCaptchaDataVo", description ="是否需要进行校验验证码")
public class CheckNeedCaptchaDataVo {

    @Schema(name ="verifyCaptcha", type ="Boolean", description ="是否需要验证码 true:是 false:否")
    private Boolean verifyCaptcha;

    @Schema(name ="id", type ="captchaId", description ="唯一标识id，用户注册接口需要传入此id")
    private Long captchaId;
}

