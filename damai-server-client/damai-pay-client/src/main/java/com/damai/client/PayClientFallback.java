package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.NotifyDto;
import com.damai.dto.PayDto;
import com.damai.dto.RefundDto;
import com.damai.dto.TradeCheckDto;
import com.damai.enums.BaseCode;
import com.damai.vo.NotifyVo;
import com.damai.vo.TradeCheckVo;
import org.springframework.stereotype.Component;

@Component
public class PayClientFallback implements PayClient {

    @Override
    public ApiResponse<String> commonPay(final PayDto payDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<NotifyVo> notify(NotifyDto notifyDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<String> refund(RefundDto refundDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<TradeCheckVo> tradeCheck(TradeCheckDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

}