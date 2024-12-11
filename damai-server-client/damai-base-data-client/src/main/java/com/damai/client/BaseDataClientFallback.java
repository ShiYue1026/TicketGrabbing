package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.GetChannelDataByCodeDto;
import com.damai.enums.BaseCode;
import com.damai.vo.GetChannelDataVo;
import org.springframework.stereotype.Component;

@Component
public class BaseDataClientFallback implements BaseDataClient{

    @Override
    public ApiResponse<GetChannelDataVo> getByCode(GetChannelDataByCodeDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
