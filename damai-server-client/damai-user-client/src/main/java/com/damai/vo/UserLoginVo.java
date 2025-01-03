package com.damai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title="UserLoginVo", description ="用户登录返回实体")
public class UserLoginVo {

    @Schema(name ="userId", type ="Long", description ="用户id")
    private Long userId;

    @Schema(name ="token", type ="String", description ="token")
    private String token;
}
