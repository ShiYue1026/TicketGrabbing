package com.damai.controller;

import com.damai.common.ApiResponse;
import com.damai.dto.AccountOrderCountDto;
import com.damai.service.OrderService;
import com.damai.vo.AccountOrderCountVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@Tag(name = "order", description = "订单")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "给定用户给定节目下的订单数量(不提供给前端调用，只允许内部program服务调用)")
    @PostMapping(value = "/account/order/count")
    public ApiResponse<AccountOrderCountVo> accountOrderCount(@Valid @RequestBody AccountOrderCountDto accountOrderCountDto) {
        return ApiResponse.ok(orderService.accountOrderCount(accountOrderCountDto));
    }
}
