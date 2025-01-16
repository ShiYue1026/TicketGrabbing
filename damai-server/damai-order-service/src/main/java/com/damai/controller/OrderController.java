package com.damai.controller;

import cn.hutool.http.server.HttpServerRequest;
import com.damai.common.ApiResponse;
import com.damai.dto.*;
import com.damai.service.OrderService;
import com.damai.vo.AccountOrderCountVo;
import com.damai.vo.OrderGetVo;
import com.damai.vo.OrderListVo;
import com.damai.vo.OrderPayCheckVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "订单创建(不提供给前端调用，只允许内部program服务调用)")
    @PostMapping(value = "/create")
    public ApiResponse<String> create(@Valid @RequestBody OrderCreateDto orderCreateDto) {
        return ApiResponse.ok(orderService.create(orderCreateDto));
    }

    @Operation(summary = "订单支付")
    @PostMapping(value = "/pay")
    public ApiResponse<String> pay(@Valid @RequestBody OrderPayDto orderPayDto) {
        return ApiResponse.ok(orderService.pay(orderPayDto));
    }

    @Operation(summary = "查看订单详情")
    @PostMapping(value = "/get")
    public ApiResponse<OrderGetVo> get(@Valid @RequestBody OrderGetDto orderGetDto) {
        return ApiResponse.ok(orderService.get(orderGetDto));
    }

    @Operation(summary = "支付宝支付后回调通知")
    @PostMapping(value = "/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        return orderService.alipayNotify(request);
    }

    @Operation(summary = "订单支付后状态检查")
    @PostMapping(value = "/pay/check")
    public ApiResponse<OrderPayCheckVo> payCheck(@Valid @RequestBody OrderPayCheckDto orderPayCheckDto) {
        return ApiResponse.ok(orderService.payCheck(orderPayCheckDto));
    }

    @Operation(summary = "查看订单列表")
    @PostMapping("/select/list")
    public ApiResponse<List<OrderListVo>> selectList(@Valid @RequestBody OrderListDto orderListDto) {
        return ApiResponse.ok(orderService.selectList(orderListDto));
    }

    @Operation(summary = "检查缓存中的订单")
    @PostMapping(value = "/get/cache")
    public ApiResponse<String> getCache(@Valid @RequestBody OrderGetDto orderGetDto) {
        return ApiResponse.ok(orderService.getCache(orderGetDto));
    }
}
