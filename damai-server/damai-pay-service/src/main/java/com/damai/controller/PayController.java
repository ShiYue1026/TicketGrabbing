package com.damai.controller;

import com.damai.common.ApiResponse;
import com.damai.dto.NotifyDto;
import com.damai.dto.PayDto;
import com.damai.dto.RefundDto;
import com.damai.dto.TradeCheckDto;
import com.damai.service.PayService;
import com.damai.vo.NotifyVo;
import com.damai.vo.TradeCheckVo;
import com.google.protobuf.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
@Tag(name = "pay", description = "支付")
public class PayController {

    @Autowired
    private PayService payService;

    @Operation(summary = "支付")
    @PostMapping(value = "/common/pay")
    public ApiResponse<String> commonPay(@Valid @RequestBody PayDto payDto){
        return ApiResponse.ok(payService.commonPay(payDto));
    }

    @Operation(summary = "支付后回调通知")
    @PostMapping("/notify")
    public ApiResponse<NotifyVo> notify(@Valid @RequestBody NotifyDto notifyDto){
        return ApiResponse.ok(payService.notify(notifyDto));
    }

    @Operation(summary = "退款")
    @PostMapping(value = "/refund")
    public ApiResponse<String> refund(@Valid @RequestBody RefundDto refundDto){
        return ApiResponse.ok(payService.refund(refundDto));
    }

    @Operation(summary = "支付状态查询")
    @PostMapping(value = "/trade/check")
    public ApiResponse<TradeCheckVo> tradeCheck(@Valid @RequestBody TradeCheckDto tradeCheckDto){
        return ApiResponse.ok(payService.tradeCheck(tradeCheckDto));
    }
}
