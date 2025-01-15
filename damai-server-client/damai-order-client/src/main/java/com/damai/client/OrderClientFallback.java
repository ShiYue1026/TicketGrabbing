package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.AccountOrderCountDto;
import com.damai.dto.OrderCreateDto;
import com.damai.enums.BaseCode;
import com.damai.vo.AccountOrderCountVo;
import org.springframework.stereotype.Component;

@Component
public class OrderClientFallback implements OrderClient {

    @Override
    public ApiResponse<AccountOrderCountVo> accountOrderCount(AccountOrderCountDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<String> create(OrderCreateDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
