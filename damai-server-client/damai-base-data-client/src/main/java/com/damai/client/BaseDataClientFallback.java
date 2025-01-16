package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.AreaGetDto;
import com.damai.dto.AreaSelectDto;
import com.damai.dto.GetChannelDataByCodeDto;
import com.damai.enums.BaseCode;
import com.damai.vo.AreaVo;
import com.damai.vo.GetChannelDataVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BaseDataClientFallback implements BaseDataClient{

    @Override
    public ApiResponse<GetChannelDataVo> getByCode(GetChannelDataByCodeDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<AreaVo> getById(AreaGetDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<List<AreaVo>> selectByIdList(AreaSelectDto areaSelectDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
