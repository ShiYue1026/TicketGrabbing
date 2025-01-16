package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.AreaGetDto;
import com.damai.dto.AreaSelectDto;
import com.damai.dto.GetChannelDataByCodeDto;
import com.damai.vo.AreaVo;
import com.damai.vo.GetChannelDataVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

import static com.damai.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"base-data-service", fallback  = BaseDataClientFallback.class)
public interface BaseDataClient {

    /**
     * 根据code查询数据
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping("/channel/data/getByCode")
    ApiResponse<GetChannelDataVo> getByCode(GetChannelDataByCodeDto dto);

    @PostMapping("/area/getById")
    ApiResponse<AreaVo> getById(AreaGetDto dto);

    @PostMapping("/area/selectByIdList")
    ApiResponse<List<AreaVo>> selectByIdList(AreaSelectDto areaSelectDto);
}
