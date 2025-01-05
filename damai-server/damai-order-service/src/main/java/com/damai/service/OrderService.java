package com.damai.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.dto.AccountOrderCountDto;
import com.damai.entity.Order;
import com.damai.mapper.OrderMapper;
import com.damai.vo.AccountOrderCountVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderMapper orderMapper;

    public AccountOrderCountVo accountOrderCount(AccountOrderCountDto accountOrderCountDto) {
        AccountOrderCountVo accountOrderCountVo = new AccountOrderCountVo();
        accountOrderCountVo.setCount(orderMapper.accountOrderCount(accountOrderCountDto.getUserId(), accountOrderCountDto.getProgramId()));
        return accountOrderCountVo;
    }
}
