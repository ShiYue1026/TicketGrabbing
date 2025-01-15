package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.client.PayClient;
import com.damai.client.UserClient;
import com.damai.common.ApiResponse;
import com.damai.core.RedisKeyManage;
import com.damai.dto.*;
import com.damai.entity.Order;
import com.damai.entity.OrderTicketUser;
import com.damai.enums.*;
import com.damai.exception.DaMaiFrameException;
import com.damai.lua.OrderProgramCacheResolutionOperate;
import com.damai.mapper.OrderMapper;
import com.damai.mapper.OrderTicketUserMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.repeatexecutelimit.annotation.RepeatExecuteLimit;
import com.damai.request.CustomizeRequestWrapper;
import com.damai.service.delaysend.DelayOperateProgramDataSend;
import com.damai.service.properties.OrderProperties;
import com.damai.servicelock.LockType;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.util.DateUtils;
import com.damai.util.ServiceLockTool;
import com.damai.util.StringUtil;
import com.damai.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.damai.constant.Constant.ALIPAY_NOTIFY_FAILURE_RESULT;
import static com.damai.constant.Constant.ALIPAY_NOTIFY_SUCCESS_RESULT;
import static com.damai.core.DistributedLockConstants.*;
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

    @Autowired
    private OrderTicketUserMapper orderTicketUserMapper;

    @Autowired
    private OrderProgramCacheResolutionOperate orderProgramCacheResolutionOperate;

    @Autowired
    private OrderProperties orderProperties;

    @Autowired
    private PayClient payClient;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ServiceLockTool serviceLockTool;

    @Lazy
    @Autowired
    private OrderService orderService;

    @Autowired
    private DelayOperateProgramDataSend delayOperateProgramDataSend;

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
        updateOrderRelatedData(orderCancelDto.getOrderNumber(), OrderStatus.CANCEL);
        return true;
    }

    /**
     * 更新订单和购票人订单状态以及操作缓存数据
     * */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderRelatedData(Long orderNumber, OrderStatus orderStatus) {
        if(!(Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode()) ||
                Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode()))) {
            throw new DaMaiFrameException(BaseCode.OPERATE_ORDER_STATUS_NOT_PERMIT);
        }
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper =
                Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, orderNumber);
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        checkOrderStatus(order);
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setOrderStatus(orderStatus.getCode());

        OrderTicketUser updateOrderTicketUser = new OrderTicketUser();
        updateOrderTicketUser.setOrderStatus(orderStatus.getCode());
        if(Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
            updateOrder.setPayOrderTime(DateUtils.now());
            updateOrderTicketUser.setPayOrderTime(DateUtils.now());
        } else if(Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
            updateOrder.setCancelOrderTime(DateUtils.now());
            updateOrderTicketUser.setCancelOrderTime(DateUtils.now());
        }

        LambdaUpdateWrapper<Order> orderLambdaUpdateWrapper = Wrappers.lambdaUpdate(Order.class)
                .eq(Order::getOrderNumber, order.getOrderNumber());
        int updateOrderResult = orderMapper.update(updateOrder, orderLambdaUpdateWrapper);
        LambdaUpdateWrapper<OrderTicketUser> orderTicketUserLambdaUpdateWrapper = Wrappers.lambdaUpdate(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, order.getOrderNumber());
        int updateTicketUserOrderResult =
                orderTicketUserMapper.update(updateOrderTicketUser, orderTicketUserLambdaUpdateWrapper);  // 可能不止一条

        if(updateOrderResult <= 0 || updateTicketUserOrderResult <= 0){
            throw new DaMaiFrameException(BaseCode.ORDER_CANAL_ERROR);
        }
        LambdaQueryWrapper<OrderTicketUser> orderTicketUserLambdaQueryWrapper = Wrappers.lambdaQuery(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, order.getOrderNumber());
        List<OrderTicketUser> orderTicketUserList = orderTicketUserMapper.selectList(orderTicketUserLambdaQueryWrapper);
        if(CollectionUtil.isEmpty(orderTicketUserList)){
            throw new DaMaiFrameException(BaseCode.TICKET_USER_ORDER_NOT_EXIST);
        }
        if(Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
            redisCache.incrBy(RedisKeyBuild.createRedisKey(
                    RedisKeyManage.ACCOUNT_ORDER_COUNT, order.getUserId(), order.getProgramId()), -updateTicketUserOrderResult);
        }

        Long programId = order.getProgramId();
        Map<Long, List<OrderTicketUser>> orderTicketUserSeatList =
                orderTicketUserList.stream().collect(Collectors.groupingBy(OrderTicketUser::getTicketCategoryId));
        Map<Long, List<Long>> seatMap = new HashMap<>(orderTicketUserSeatList.size());
        orderTicketUserSeatList.forEach((k, v) -> {
            seatMap.put(k, v.stream().map(OrderTicketUser::getSeatId).collect(Collectors.toList()));
        });

        //更新缓存中座位相关的数据
        updateProgramRelatedDataResolution(programId, seatMap, orderStatus);
    }

    public void updateProgramRelatedDataResolution(Long programId, Map<Long, List<Long>> seatMap, OrderStatus orderStatus) {
        log.info("updateProgramRelatedDataResolution");
        Map<Long, List<SeatVo>> seatVoMap = new HashMap<>(seatMap.size());
        seatMap.forEach((k,v) -> seatVoMap.put(k,redisCache.multiGetForHash(
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, programId, k),
                v.stream().map(String::valueOf).collect(Collectors.toList()), SeatVo.class)));
        if(CollectionUtil.isEmpty(seatVoMap)) {
            throw new DaMaiFrameException(BaseCode.LOCK_SEAT_LIST_EMPTY);
        }
        JSONArray jsonArray = new JSONArray();
        JSONArray addSeatDatajsonArray = new JSONArray();
        List<TicketCategoryCountDto> ticketCategoryCountDtoList = new ArrayList<>();
        JSONArray unLockSeatIdjsonArray = new JSONArray();
        List<Long> unLockSeatIdList = new ArrayList<>();
        seatVoMap.forEach((k, v) -> {
            JSONObject unLockSeatIdjsonObject = new JSONObject();
            unLockSeatIdjsonObject.put("programSeatLockHashKey", RedisKeyBuild.createRedisKey(
                    RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, programId, k).getRelKey());
            unLockSeatIdjsonObject.put("unLockSeatIdList", v.stream()
                    .map(SeatVo::getId).map(String::valueOf).collect(Collectors.toList()));
            unLockSeatIdjsonArray.add(unLockSeatIdjsonObject);
            JSONObject seatDatajsonObject = new JSONObject();
            String seatHashKeyAdd = "";
            if(Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
                seatHashKeyAdd = RedisKeyBuild.createRedisKey(
                        RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH, programId, k).getRelKey();
                for (SeatVo seatVo : v) {
                    seatVo.setSellStatus(SellStatus.NO_SOLD.getCode());
                }
            } else if(Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
                seatHashKeyAdd = RedisKeyBuild.createRedisKey(
                        RedisKeyManage.PROGRAM_SEAT_SOLD_RESOLUTION_HASH, programId, k).getRelKey();
                for (SeatVo seatVo : v) {
                    seatVo.setSellStatus(SellStatus.SOLD.getCode());
                }
            }
            seatDatajsonObject.put("seatHashKeyAdd", seatHashKeyAdd);
            List<String> seatDataList = new ArrayList<>();
            for (SeatVo seatVo : v) {
                seatDataList.add(String.valueOf(seatVo.getId()));
                seatDataList.add(JSON.toJSONString(seatVo));
            }
            seatDatajsonObject.put("seatDataList", seatDataList);
            addSeatDatajsonArray.add(seatDatajsonObject);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("programTicketRemainNumberHashKey",RedisKeyBuild.createRedisKey(
                    RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION, programId, k).getRelKey());
            jsonObject.put("ticketCategoryId",String.valueOf(k));
            jsonObject.put("count",v.size());
            jsonArray.add(jsonObject);
            TicketCategoryCountDto ticketCategoryCountDto = new TicketCategoryCountDto();
            ticketCategoryCountDto.setTicketCategoryId(k);
            ticketCategoryCountDto.setCount((long) v.size());
            ticketCategoryCountDtoList.add(ticketCategoryCountDto);
            unLockSeatIdList.addAll(v.stream().map(SeatVo::getId).collect(Collectors.toList()));
        });
        List<String> keys = new ArrayList<>();
        keys.add(String.valueOf(orderStatus.getCode()));
        Object[] data = new String[3];
        data[0] = JSON.toJSONString(unLockSeatIdjsonArray);
        data[1] = JSON.toJSONString(addSeatDatajsonArray);
        data[2] = JSON.toJSONString(jsonArray);
        orderProgramCacheResolutionOperate.programCacheReverseOperate(keys,data);
        if (Objects.equals(orderStatus.getCode(), OrderStatus.PAY.getCode())) {
            ProgramOperateDataDto programOperateDataDto = new ProgramOperateDataDto();
            programOperateDataDto.setProgramId(programId);
            programOperateDataDto.setSellStatus(SellStatus.SOLD.getCode());
            programOperateDataDto.setSeatIdList(unLockSeatIdList);
            programOperateDataDto.setTicketCategoryCountDtoList(ticketCategoryCountDtoList);
            delayOperateProgramDataSend.sendMessage(JSON.toJSONString(programOperateDataDto));
            log.info("延迟操作节目消息发送 消息体: {}", programOperateDataDto);
        }
    }

    private void checkOrderStatus(Order order) {
        if(Objects.isNull(order)){
            throw new DaMaiFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        if(Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())){
            throw new DaMaiFrameException(BaseCode.ORDER_CANCEL);
        }
        if(Objects.equals(order.getOrderStatus(), OrderStatus.PAY.getCode())){
            throw new DaMaiFrameException(BaseCode.ORDER_PAY);
        }
        if(Objects.equals(order.getOrderStatus(), OrderStatus.REFUND.getCode())){
            throw new DaMaiFrameException(BaseCode.ORDER_REFUND);
        }
    }

    public String pay(OrderPayDto orderPayDto) {
        Long orderNumber = orderPayDto.getOrderNumber();
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper = Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNumber, orderNumber);
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);

        if(Objects.isNull(order)){
            throw new DaMaiFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        if(Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())) {
            throw new DaMaiFrameException(BaseCode.ORDER_CANCEL);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.PAY.getCode())) {
            throw new DaMaiFrameException(BaseCode.ORDER_PAY);
        }
        if (Objects.equals(order.getOrderStatus(), OrderStatus.REFUND.getCode())) {
            throw new DaMaiFrameException(BaseCode.ORDER_REFUND);
        }
        if(orderPayDto.getPrice().compareTo(order.getOrderPrice()) != 0){
            throw new DaMaiFrameException(BaseCode.PAY_PRICE_NOT_EQUAL_ORDER_PRICE);
        }
        PayDto payDto = getPayDto(orderPayDto, orderNumber);
        ApiResponse<String> payResponse = payClient.commonPay(payDto);
        if(!Objects.equals(payResponse.getCode(), BaseCode.SUCCESS.getCode())){
            throw new DaMaiFrameException(payResponse);
        }
        return payResponse.getData();
    }

    private PayDto getPayDto(OrderPayDto orderPayDto, Long orderNumber) {
        PayDto payDto = new PayDto();
        payDto.setOrderNumber(String.valueOf(orderPayDto.getOrderNumber()));
        payDto.setPayBillType(orderPayDto.getPayBillType());
        payDto.setSubject(orderPayDto.getSubject());
        payDto.setChannel(orderPayDto.getChannel());
        payDto.setPlatform(orderPayDto.getPlatform());
        payDto.setPrice(orderPayDto.getPrice());
        payDto.setNotifyUrl(orderProperties.getOrderPayNotifyUrl());
        payDto.setReturnUrl(orderProperties.getOrderPayReturnUrl());
        return payDto;
    }

    public OrderGetVo get(OrderGetDto orderGetDto) {
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper = Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNumber, orderGetDto.getOrderNumber());
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        if(Objects.isNull(order)){
            throw new DaMaiFrameException(BaseCode.ORDER_NOT_EXIST);
        }

        LambdaQueryWrapper<OrderTicketUser> orderTicketUserLambdaQueryWrapper = Wrappers.lambdaQuery(OrderTicketUser.class)
                .eq(OrderTicketUser::getOrderNumber, orderGetDto.getOrderNumber());
        List<OrderTicketUser> orderTicketUserList = orderTicketUserMapper.selectList(orderTicketUserLambdaQueryWrapper);
        if(CollectionUtil.isEmpty(orderTicketUserList)){
            throw new DaMaiFrameException(BaseCode.TICKET_USER_ORDER_NOT_EXIST);
        }

        OrderGetVo orderGetVo = new OrderGetVo();
        BeanUtil.copyProperties(order, orderGetVo);

        List<OrderTicketInfoVo> orderTicketInfoVoList = new ArrayList<>();
        Map<BigDecimal, List<OrderTicketUser>> orderTicketUserMap = orderTicketUserList.stream()
                .collect(Collectors.groupingBy(OrderTicketUser::getOrderPrice));
        orderTicketUserMap.forEach((k, v) -> {
            OrderTicketInfoVo orderTicketInfoVo = new OrderTicketInfoVo();
            String seatInfo = v.stream().map(OrderTicketUser::getSeatInfo).collect(Collectors.joining(","));
            orderTicketInfoVo.setSeatInfo(seatInfo);
            orderTicketInfoVo.setPrice(k);
            orderTicketInfoVo.setQuantity(v.size());
            orderTicketInfoVo.setRelPrice(v.stream().map(OrderTicketUser::getOrderPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            orderTicketInfoVoList.add(orderTicketInfoVo);
        });

        orderGetVo.setOrderTicketInfoVoList(orderTicketInfoVoList);

        UserGetAndTicketUserListDto userGetAndTicketUserListDto = new UserGetAndTicketUserListDto();
        userGetAndTicketUserListDto.setUserId(order.getUserId());
        ApiResponse<UserGetAndTicketUserListVo> userGetAndTicketUserApiResponse =
                userClient.getUserAndTicketUserList(userGetAndTicketUserListDto);

        if(!Objects.equals(userGetAndTicketUserApiResponse.getCode(), BaseCode.SUCCESS.getCode())){
            throw new DaMaiFrameException(userGetAndTicketUserApiResponse);
        }
        UserGetAndTicketUserListVo userGetAndTicketUserListVo =
                Optional.ofNullable(userGetAndTicketUserApiResponse.getData())
                        .orElseThrow(() -> new DaMaiFrameException(BaseCode.RPC_RESULT_DATA_EMPTY));
        if(Objects.isNull(userGetAndTicketUserListVo.getUserVo())){
            throw new DaMaiFrameException(BaseCode.USER_EMPTY);
        }
        if(CollectionUtil.isEmpty(userGetAndTicketUserListVo.getTicketUserVoList())) {
            throw new DaMaiFrameException(BaseCode.TICKET_USER_EMPTY);
        }
        List<TicketUserVo> filterTicketUserVoList = new ArrayList<>();
        Map<Long, TicketUserVo> ticketUserVoMap = userGetAndTicketUserListVo.getTicketUserVoList()
                .stream().collect(Collectors.toMap(TicketUserVo::getId, ticketUserVo -> ticketUserVo, (v1, v2) -> v2));
        // 将购票的购票人加入列表中
        for (OrderTicketUser orderTicketUser : orderTicketUserList) {
            filterTicketUserVoList.add(ticketUserVoMap.get(orderTicketUser.getTicketUserId()));
        }

        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtil.copyProperties(userGetAndTicketUserListVo.getUserVo(), userInfoVo);
        UserAndTicketUserInfoVo userAndTicketUserInfoVo = new UserAndTicketUserInfoVo();
        userAndTicketUserInfoVo.setUserInfoVo(userInfoVo);
        userAndTicketUserInfoVo.setTicketUserInfoVoList(BeanUtil.copyToList(filterTicketUserVoList, TicketUserInfoVo.class));
        orderGetVo.setUserAndTicketUserInfoVo(userAndTicketUserInfoVo);

        return orderGetVo;
    }

    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>(256);
        if(request instanceof final CustomizeRequestWrapper customizeRequestWrapper) {
            String requestBody = customizeRequestWrapper.getRequestBody();
            params = StringUtil.convertQueryStringToMap(requestBody);
        }
        log.info("收到支付宝回调通知 params : {}", JSON.toJSONString(params));
        String outTradeNo = params.get("out_trade_no");
        if(StringUtil.isEmpty(outTradeNo)){
            return ALIPAY_NOTIFY_FAILURE_RESULT;
        }

        RLock lock = serviceLockTool.getLock(LockType.Reentrant, ORDER_PAY_CHECK, new String[]{outTradeNo});
        lock.lock();
        try{
            LambdaQueryWrapper<Order> lambdaQueryWrapper = Wrappers.lambdaQuery(Order.class)
                    .eq(Order::getOrderNumber, Long.parseLong(outTradeNo));
            Order order = orderMapper.selectOne(lambdaQueryWrapper);
            if(Objects.isNull(order)){
                throw new DaMaiFrameException(BaseCode.ORDER_NOT_EXIST);
            }
            if(Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())) {  // 用户要正好卡在订单要关闭的时候，还要停留在支付宝/微信的支付页面，等待订单关闭了，再支付 需要退款
                RefundDto refundDto = new RefundDto();
                refundDto.setOrderNumber(outTradeNo);
                refundDto.setAmount(order.getOrderPrice());
                refundDto.setChannel("alipay");
                refundDto.setReason("延迟订单关闭");
                ApiResponse<String> response = payClient.refund(refundDto);
                if(response.getCode().equals(BaseCode.SUCCESS.getCode())){
                    Order updateOrder = new Order();
                    updateOrder.setEditTime(DateUtils.now());
                    updateOrder.setOrderStatus(OrderStatus.REFUND.getCode());
                    LambdaUpdateWrapper<Order> lambdaUpdateWrapper = Wrappers.lambdaUpdate(Order.class)
                            .eq(Order::getOrderNumber, outTradeNo);
                    orderMapper.update(updateOrder, lambdaUpdateWrapper);
                } else{
                    log.error("pay服务退款失败 dto : {} response : {}", com.alibaba.fastjson.JSON.toJSONString(refundDto), com.alibaba.fastjson.JSON.toJSONString(response));
                }
                return ALIPAY_NOTIFY_SUCCESS_RESULT;
            }

            NotifyDto notifyDto = new NotifyDto();
            notifyDto.setChannel(PayChannel.ALIPAY.getValue());
            notifyDto.setParams(params);
            ApiResponse<NotifyVo> notifyVoApiResponse = payClient.notify(notifyDto);  // 验签、验证参数后并更新账单状态
            if(!Objects.equals(notifyVoApiResponse.getCode(), BaseCode.SUCCESS.getCode())){
                throw new DaMaiFrameException(notifyVoApiResponse);
            }

            if(ALIPAY_NOTIFY_SUCCESS_RESULT.equals(notifyVoApiResponse.getData().getPayResult())) {
                try{
                    orderService.updateOrderRelatedData(Long.parseLong(notifyVoApiResponse.getData().getOutTradeNo()), OrderStatus.PAY);
                }catch (Exception e){
                    log.warn("updateOrderRelatedData warn message",e);
                }
            }
            return notifyVoApiResponse.getData().getPayResult();
        } finally {
            lock.unlock();
        }
    }

    @ServiceLock(name = ORDER_PAY_CHECK, keys = {"#orderPayCheckDto.orderNumber"})
    public OrderPayCheckVo payCheck(OrderPayCheckDto orderPayCheckDto) {
        OrderPayCheckVo orderPayCheckVo = new OrderPayCheckVo();

        LambdaQueryWrapper<Order> orderLambdaQueryWrapper = Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNumber, orderPayCheckDto.getOrderNumber());
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        if(Objects.isNull(order)){
            throw new DaMaiFrameException(BaseCode.ORDER_NOT_EXIST);
        }
        BeanUtil.copyProperties(order, orderPayCheckVo);
        if(Objects.equals(order.getOrderStatus(), OrderStatus.CANCEL.getCode())) {  // 支付的时候正好延迟取消订单消息队列消费了，订单已取消，需要退款
            RefundDto refundDto = new RefundDto();
            refundDto.setOrderNumber(String.valueOf(order.getOrderNumber()));
            refundDto.setAmount(order.getOrderPrice());
            refundDto.setChannel("alipay");
            refundDto.setReason("延迟订单关闭");
            ApiResponse<String> response = payClient.refund(refundDto);
            if(response.getCode().equals(BaseCode.SUCCESS.getCode())){
                Order updateOrder = new Order();
                updateOrder.setEditTime(DateUtils.now());
                updateOrder.setOrderStatus(OrderStatus.REFUND.getCode());
                LambdaUpdateWrapper<Order> lambdaUpdateWrapper = Wrappers.lambdaUpdate(Order.class)
                        .eq(Order::getOrderNumber, order.getOrderNumber());
                orderMapper.update(updateOrder, lambdaUpdateWrapper);
            } else{
                log.error("pay服务退款失败 dto : {} response : {}", com.alibaba.fastjson.JSON.toJSONString(refundDto), com.alibaba.fastjson.JSON.toJSONString(response));
            }
            orderPayCheckVo.setOrderStatus(OrderStatus.REFUND.getCode());
            orderPayCheckVo.setCancelOrderTime(DateUtils.now());
            return orderPayCheckVo;
        }

        TradeCheckDto tradeCheckDto = new TradeCheckDto();
        tradeCheckDto.setOutTradeNo(String.valueOf(orderPayCheckDto.getOrderNumber()));
        tradeCheckDto.setChannel(Optional.ofNullable(PayChannel.getRc(orderPayCheckDto.getPayChannelType()))
                .map(PayChannel::getValue).orElseThrow(() -> new DaMaiFrameException(BaseCode.PAY_CHANNEL_NOT_EXIST)));
        ApiResponse<TradeCheckVo> tradeCheckVoApiResponse = payClient.tradeCheck(tradeCheckDto);
        if(!Objects.equals(tradeCheckVoApiResponse.getCode(), BaseCode.SUCCESS.getCode())){
            throw new DaMaiFrameException(tradeCheckVoApiResponse);
        }
        TradeCheckVo tradeCheckVo = Optional.ofNullable(tradeCheckVoApiResponse.getData())
                .orElseThrow(() -> new DaMaiFrameException(BaseCode.PAY_BILL_NOT_EXIST));
        if(tradeCheckVo.isSuccess()){
            Integer payBillStatus = tradeCheckVo.getPayBillStatus();
            Integer orderStatus = order.getOrderStatus();
            if(!Objects.equals(payBillStatus, orderStatus)){  // 订单状态和账单状态不一致，更新
                orderPayCheckVo.setOrderStatus(payBillStatus);
                try{
                    if(Objects.equals(payBillStatus, PayBillStatus.PAY.getCode())){
                        orderPayCheckVo.setPayOrderTime(DateUtils.now());
                        orderService.updateOrderRelatedData(order.getOrderNumber(), OrderStatus.PAY);
                    } else if(Objects.equals(payBillStatus, PayBillStatus.CANCEL.getCode())) {
                        orderPayCheckVo.setCancelOrderTime(DateUtils.now());
                        orderService.updateOrderRelatedData(order.getOrderNumber(), OrderStatus.CANCEL);
                    }
                } catch (Exception e){
                    log.warn("updateOrderRelatedData warn message",e);
                }
            }
        } else{
            throw new DaMaiFrameException(BaseCode.PAY_TRADE_CHECK_ERROR);
        }
        return orderPayCheckVo;
    }
}
