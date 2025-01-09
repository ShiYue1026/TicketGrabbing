package com.damai.service.strategy;

import com.damai.dto.ProgramOrderCreateDto;

public interface ProgramOrderStrategy {

    /**
     * 创建订单
     * @param programOrderCreateDto 订单参数
     * @return 订单编号
     * */
    String createOrder(ProgramOrderCreateDto programOrderCreateDto);

}
