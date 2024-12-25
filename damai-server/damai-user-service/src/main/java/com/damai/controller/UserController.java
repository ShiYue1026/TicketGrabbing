package com.damai.controller;

import com.damai.common.ApiResponse;
import com.damai.dto.UserRegisterDto;
import com.damai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
}
