package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.core.RedisKeyManage;
import com.damai.dto.AccountOrderCountDto;
import com.damai.dto.OrderCancelDto;
import com.damai.dto.OrderCreateDto;
import com.damai.dto.OrderTicketUserCreateDto;
import com.damai.entity.Order;
import com.damai.entity.OrderTicketUser;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.OrderMapper;
import com.damai.mapper.OrderTicketUserMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.repeatexecutelimit.annotation.RepeatExecuteLimit;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.vo.AccountOrderCountVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.damai.core.DistributedLockConstants.ORDER_CANCEL_LOCK;
import static com.damai.core.RepeatExecuteLimitConstants.CANCEL_PROGRAM_ORDER;

@Slf4j
@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UidGenerator uidGenerator;

    @Autowired
    private OrderTicketUserService orderTicketUserService;

    @Autowired
    private RedisCache redisCache;

    public AccountOrderCountVo accountOrderCount(AccountOrderCountDto accountOrderCountDto) {
        AccountOrderCountVo accountOrderCountVo = new AccountOrderCountVo();
        accountOrderCountVo.setCount(orderMapper.accountOrderCount(accountOrderCountDto.getUserId(), accountOrderCountDto.getProgramId()));
        return accountOrderCountVo;
    }

    @Transactional(rollbackFor = Exception.class)
    public String create(OrderCreateDto orderCreateDto) {
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper = Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNumber, orderCreateDto.getOrderNumber());
        Order oldOrder = orderMapper.selectOne(orderLambdaQueryWrapper);
        if (Objects.nonNull(oldOrder)) {
            throw new DaMaiFrameException(BaseCode.ORDER_EXIST);
        }
        Order order = new Order();
        BeanUtil.copyProperties(orderCreateDto, order);
        order.setDistributionMode("电子票");
        order.setTakeTicketMode("请使用购票人身份证直接入场");
        orderMapper.insert(order);

        List<OrderTicketUser> orderTicketUserList = new ArrayList<>();
        for (OrderTicketUserCreateDto orderTicketUserCreateDto : orderCreateDto.getOrderTicketUserCreateDtoList()) {
            OrderTicketUser orderTicketUser = new OrderTicketUser();
            BeanUtil.copyProperties(orderTicketUserCreateDto, orderTicketUser);
            orderTicketUser.setId(uidGenerator.getUid());
            orderTicketUserList.add(orderTicketUser);
        }
        orderTicketUserService.saveBatch(orderTicketUserList);

        // 更新当前用户的订单数量
        redisCache.incrBy(RedisKeyBuild.createRedisKey(
                RedisKeyManage.ACCOUNT_ORDER_COUNT,
                orderCreateDto.getUserId(),
                orderCreateDto.getProgramId()),
                orderCreateDto.getOrderTicketUserCreateDtoList().size());

        return String.valueOf(order.getOrderNumber());
    }

    @RepeatExecuteLimit(name = CANCEL_PROGRAM_ORDER,keys = {"#orderCancelDto.orderNumber"})
    @ServiceLock(name = ORDER_CANCEL_LOCK,keys = {"#orderCancelDto.orderNumber"})
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(OrderCancelDto orderCancelDto) {
        // TODO
        return true;
    }
}
