package com.damai.controller;

import com.damai.common.ApiResponse;
import com.damai.dto.UserIdDto;
import com.damai.dto.UserLoginDto;
import com.damai.dto.UserLogoutDto;
import com.damai.dto.UserRegisterDto;
import com.damai.service.UserService;
import com.damai.vo.UserLoginVo;
import com.damai.vo.UserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Tag(name = "user", description = "用户")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "注册")
    @PostMapping(value = "/register")
    public ApiResponse<Boolean> response(@Valid @RequestBody UserRegisterDto userRegisterDto) {
        return ApiResponse.ok(userService.register(userRegisterDto));
    }

    @Operation(summary = "登录")
    @PostMapping(value = "/login")
    public ApiResponse<UserLoginVo> login(@Valid @RequestBody UserLoginDto userLoginDto) {
        return ApiResponse.ok(userService.login(userLoginDto));
    }

    @Operation(summary = "退出登录")
    @PostMapping(value = "/logout")
    public ApiResponse<Boolean> logout(@Valid @RequestBody UserLogoutDto userLogoutDto) {
        return ApiResponse.ok(userService.logout(userLogoutDto));
    }

    @Operation(summary = "查询(通过id)")
    @PostMapping(value = "/get/id")
    public ApiResponse<UserVo> getById(@Valid @RequestBody UserIdDto userIdDto) {
        return ApiResponse.ok(userService.getById(userIdDto));
    }
}
