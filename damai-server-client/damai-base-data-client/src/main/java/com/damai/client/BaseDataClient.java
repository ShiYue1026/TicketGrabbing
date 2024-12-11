package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.GetChannelDataByCodeDto;
import com.damai.vo.GetChannelDataVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import static com.damai.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"base-data-service", fallback  = BaseDataClientFallback.class)
public interface BaseDataClient {

    @PostMapping("/channel/data/getByCode")
    ApiResponse<GetChannelDataVo> getByCode(GetChannelDataByCodeDto dto);


}
