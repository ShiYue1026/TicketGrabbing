package com.damai.controller;

import com.damai.common.ApiResponse;
import com.damai.dto.ProgramOrderCreateDto;
import com.damai.enums.ProgramOrderVersion;
import com.damai.service.ProgramOrderService;
import com.damai.service.strategy.ProgramOrderContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/program/order")
@Tag(name = "program-order", description = "节目订单")
public class ProgramOrderController {

    @Autowired
    private ProgramOrderService programOrderService;

    @Operation(summary = "购票V1")
    @PostMapping(value = "/create/v1")
    public ApiResponse<String> createV1(@Valid @RequestBody ProgramOrderCreateDto programOrderCreateDto) {
        return ApiResponse.ok(programOrderService.createV1(programOrderCreateDto));
    }
}
