package com.damai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title="UserInfoVo", description ="用户数据")
public class UserInfoVo {

    @Schema(name ="id", type ="String", description ="用户id")
    private Long id;

    @Schema(name ="name", type ="String", description ="用户名字")
    private String name;

    @Schema(name ="relName", type ="String", description ="用户真实名字")
    private String relName;

    @Schema(name ="gender", type ="Integer", description ="1:男 2:女")
    private Integer gender;

    @Schema(name ="name", type ="String", description ="用户手机号")
    private String mobile;

    @Schema(name ="address", type ="String", description ="收货地址")
    private String address;
}
