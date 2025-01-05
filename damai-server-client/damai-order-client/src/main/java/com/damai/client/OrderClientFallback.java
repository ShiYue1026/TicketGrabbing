package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.AccountOrderCountDto;
import com.damai.enums.BaseCode;
import com.damai.vo.AccountOrderCountVo;

public class OrderClientFallback implements OrderClient {

    @Override
    public ApiResponse<AccountOrderCountVo> accountOrderCount(AccountOrderCountDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
