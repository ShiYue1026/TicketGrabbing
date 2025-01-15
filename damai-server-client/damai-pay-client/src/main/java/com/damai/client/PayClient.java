package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.NotifyDto;
import com.damai.dto.PayDto;
import com.damai.dto.RefundDto;
import com.damai.dto.TradeCheckDto;
import com.damai.vo.NotifyVo;
import com.damai.vo.TradeCheckVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import static com.damai.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"pay-service",fallback = PayClientFallback.class)
public interface PayClient {

    /**
     * 支付
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/common/pay")
    ApiResponse<String> commonPay(PayDto dto);

    /**
     * 回调
     * @param notifyDto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/notify")
    ApiResponse<NotifyVo> notify(NotifyDto notifyDto);

    /**
     * 退款
     * @param refundDto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/refund")
    ApiResponse<String> refund(RefundDto refundDto);

    /**
     * 查询支付状态
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/pay/trade/check")
    ApiResponse<TradeCheckVo> tradeCheck(TradeCheckDto dto);
}
