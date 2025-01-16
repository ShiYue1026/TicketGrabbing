package com.damai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.damai.entity.OrderTicketUser;
import com.damai.entity.OrderTicketUserAggregate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderTicketUserMapper extends BaseMapper<OrderTicketUser> {

    List<OrderTicketUserAggregate> selectOrderTicketUserAggregate(@Param("orderNumberList") List<Long> orderNumberList);
}
