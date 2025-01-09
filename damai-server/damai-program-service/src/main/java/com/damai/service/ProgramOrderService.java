package com.damai.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baidu.fsg.uid.UidGenerator;
import com.damai.client.OrderClient;
import com.damai.common.ApiResponse;
import com.damai.core.RedisKeyManage;
import com.damai.core.RepeatExecuteLimitConstants;
import com.damai.dto.*;
import com.damai.entity.ProgramShowTime;
import com.damai.entity.Seat;
import com.damai.enums.BaseCode;
import com.damai.enums.CompositeCheckType;
import com.damai.enums.OrderStatus;
import com.damai.enums.SellStatus;
import com.damai.exception.DaMaiFrameException;
import com.damai.initialize.impl.composite.CompositeContainer;
import com.damai.lua.ProgramCacheResolutionOperate;
import com.damai.redis.RedisKeyBuild;
import com.damai.repeatexecutelimit.annotation.RepeatExecuteLimit;
import com.damai.service.delaysend.DelayOrderCancelSend;
import com.damai.service.tool.SeatMatch;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.util.DateUtils;
import com.damai.vo.ProgramVo;
import com.damai.vo.SeatVo;
import com.damai.vo.TicketCategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.damai.core.DistributedLockConstants.PROGRAM_ORDER_CREATE_V1;
import static com.damai.service.constant.ProgramOrderConstant.ORDER_TABLE_COUNT;

@Slf4j
@Service
public class ProgramOrderService {

    @Autowired
    private ProgramShowTimeService programShowTimeService;

    @Autowired
    private TicketCategoryService ticketCategoryService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private ProgramCacheResolutionOperate programCacheResolutionOperate;

    @Autowired
    private ProgramService programService;

    @Autowired
    private DelayOrderCancelSend delayOrderCancelSend;

    @Autowired
    private UidGenerator uidGenerator;

    @Qualifier("com.damai.client.OrderClient")
    @Autowired
    private OrderClient orderClient;

    @Autowired
    private CompositeContainer compositeContainer;

    @RepeatExecuteLimit(
            name = RepeatExecuteLimitConstants.CREATE_PROGRAM_ORDER,
            keys = {"#programOrderCreateDto.userId","#programOrderCreateDto.programId"},
            durationTime = 3)
    @ServiceLock(name = PROGRAM_ORDER_CREATE_V1,keys = {"#programOrderCreateDto.programId"})
    public String createV1(ProgramOrderCreateDto programOrderCreateDto) {

        compositeContainer.execute(CompositeCheckType.PROGRAM_ORDER_CREATE_CHECK.getValue(),programOrderCreateDto);

        // 从多级缓存中查询节目演出时间
        ProgramShowTime programShowTime = programShowTimeService.selectProgramShowTimeByProgramIdMultipleCache(programOrderCreateDto.getProgramId());

        // 查询对应的票档类型
        List<TicketCategoryVo> getTicketCategoryList = getTicketCategoryList(programOrderCreateDto, programShowTime.getShowTime());

        // 计算传入的座位的总价格
        BigDecimal parameterOrderPrice = new BigDecimal(0);
        //库中座位的总价格
        BigDecimal databaseOrderPrice = new BigDecimal(0);

        // 要购买的座位
        List<SeatVo> purchaseSeatList = new ArrayList<>();
        // 入参的座位
        List<SeatDto> seatDtoList = programOrderCreateDto.getSeatDtoList();
        // 当前节目下所有未售卖的座位
        List<SeatVo> seatVoList = new ArrayList<>();
        // 该节目下的余票数量
        Map<String, Long> ticketCategoryRemainNumber = new HashMap<>(16);

        for (TicketCategoryVo ticketCategory : getTicketCategoryList) {  // 遍历用户选择的所有票档
            // 查询该节目该票档下的所有座位
            List<SeatVo> allSeatVoList = seatService.selectSeatResolution(
                    programOrderCreateDto.getProgramId(),
                    programOrderCreateDto.getTicketCategoryId(),
                    DateUtils.countBetweenSecond(DateUtils.now(), programShowTime.getShowTime()),
                    TimeUnit.SECONDS
                    );
            // 将所有未售卖的座位放入seatVoList
            seatVoList.addAll(allSeatVoList.stream().filter(seatVo -> seatVo.getSellStatus().equals(SellStatus.NO_SOLD.getCode())).toList());
            // 将查询到的余票数量放入ticketCategoryRemainNumber (票档id, 余票数量)
            ticketCategoryRemainNumber.putAll(ticketCategoryService.getRedisRemainNumberResolution(
                    programOrderCreateDto.getProgramId(),
                    ticketCategory.getId()
            ));
        }

        // 手动选座
        if(CollectionUtil.isNotEmpty(seatDtoList)) {
            // 余票数量检测
            Map<Long, Long> seatTicketCategoryDtoCount = seatDtoList.stream()
                    .collect(Collectors.groupingBy(SeatDto::getTicketCategoryId, Collectors.counting()));
            for(Entry<Long, Long> entry: seatTicketCategoryDtoCount.entrySet()) {
                Long ticketCategoryId = entry.getKey();
                Long purchaseCount = entry.getValue();
                // 余票数量
                Long remainNumber = Optional.ofNullable(ticketCategoryRemainNumber.get(String.valueOf(ticketCategoryId)))
                        .orElseThrow(() -> new DaMaiFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2));
                if(purchaseCount > remainNumber) {
                    throw new DaMaiFrameException(BaseCode.TICKET_REMAIN_NUMBER_NOT_SUFFICIENT);
                }
            }

            for (SeatDto seatDto : seatDtoList) {
                Map<String, SeatVo> seatVoMap = seatVoList.stream()
                        .collect(Collectors.toMap(seat -> seat.getRowCode() + "-" + seat.getColCode(), seat -> seat, (v1, v2) -> v2));
                SeatVo seatVo = seatVoMap.get(seatDto.getRowCode() + "-" + seatDto.getColCode());
                if(Objects.isNull(seatVo)) {
                    throw new DaMaiFrameException(BaseCode.SEAT_IS_NOT_NOT_SOLD);
                }
                purchaseSeatList.add(seatVo);
                parameterOrderPrice = parameterOrderPrice.add(seatDto.getPrice());
                databaseOrderPrice = databaseOrderPrice.add(seatVo.getPrice());
            }
            if(parameterOrderPrice.compareTo(databaseOrderPrice) > 0) {
                throw new DaMaiFrameException(BaseCode.PRICE_ERROR);
            }
        }
        // 自动选座
        else{
            Long ticketCategoryId = programOrderCreateDto.getTicketCategoryId();
            Integer ticketCount = programOrderCreateDto.getTicketCount();
            Long remainNumber = Optional.ofNullable(ticketCategoryRemainNumber.get(String.valueOf(ticketCategoryId)))
                    .orElseThrow(() -> new DaMaiFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2));
            if(ticketCount > remainNumber) {
                throw new DaMaiFrameException(BaseCode.TICKET_REMAIN_NUMBER_NOT_SUFFICIENT);
            }
            purchaseSeatList = SeatMatch.findAdjacentSeatVos(seatVoList.stream()
                    .filter(seatVo -> Objects.equals(seatVo.getTicketCategoryId(), ticketCategoryId)).collect(Collectors.toList()), ticketCount);
            if(purchaseSeatList.size() < ticketCount) {
                throw new DaMaiFrameException(BaseCode.SEAT_OCCUPY);
            }
        }
        updateProgramCacheDataResolution(programOrderCreateDto.getProgramId(), purchaseSeatList, OrderStatus.NO_PAY);
        return doCreate(programOrderCreateDto, purchaseSeatList);
    }

    private String doCreate(ProgramOrderCreateDto programOrderCreateDto, List<SeatVo> purchaseSeatList) {
        OrderCreateDto orderCreateDto = buildCreateOrderParam(programOrderCreateDto, purchaseSeatList);

        String orderNumber = createOrderByRpc(orderCreateDto, purchaseSeatList);

        DelayOrderCancelDto delayOrderCancelDto = new DelayOrderCancelDto();
        delayOrderCancelDto.setOrderNumber(orderCreateDto.getOrderNumber());
        delayOrderCancelSend.sendMessage(JSON.toJSONString(delayOrderCancelDto));

        return orderNumber;
    }

    private String createOrderByRpc(OrderCreateDto orderCreateDto, List<SeatVo> purchaseSeatList) {
        ApiResponse<String> createOrderResponse = orderClient.create(orderCreateDto);
        if(!Objects.equals(createOrderResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            log.error("创建订单失败 需人工处理 orderCreateDto : {}", com.alibaba.fastjson.JSON.toJSONString(orderCreateDto));
            updateProgramCacheDataResolution(orderCreateDto.getProgramId(),purchaseSeatList,OrderStatus.CANCEL);
            throw new DaMaiFrameException(createOrderResponse);
        }
        return createOrderResponse.getData();
    }

    private OrderCreateDto buildCreateOrderParam(ProgramOrderCreateDto programOrderCreateDto, List<SeatVo> purchaseSeatList) {
        ProgramVo programVo = programService.simpleGetProgramAndShowMultipleCache(programOrderCreateDto.getProgramId());
        // 生成主订单
        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setOrderNumber(uidGenerator.getOrderNumber(programOrderCreateDto.getUserId(), ORDER_TABLE_COUNT));  // 基因法生成订单id
        orderCreateDto.setProgramId(programOrderCreateDto.getProgramId());
        orderCreateDto.setProgramItemPicture(programVo.getItemPicture());
        orderCreateDto.setUserId(programOrderCreateDto.getUserId());
        orderCreateDto.setProgramTitle(programVo.getTitle());
        orderCreateDto.setProgramPlace(programVo.getPlace());
        orderCreateDto.setProgramShowTime(programVo.getShowTime());
        orderCreateDto.setProgramPermitChooseSeat(programVo.getPermitChooseSeat());
        BigDecimal databaseOrderPrice =
                purchaseSeatList.stream().map(SeatVo::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        orderCreateDto.setOrderPrice(databaseOrderPrice);
        orderCreateDto.setCreateOrderTime(DateUtils.now());

        // 生成每个购票人订单
        List<Long> ticketUserIdList = programOrderCreateDto.getTicketUserIdList();
        List<OrderTicketUserCreateDto> orderTicketUserCreateDtoList = new ArrayList<>();
        for (int i = 0; i < ticketUserIdList.size(); i++) {
            Long ticketUserId = ticketUserIdList.get(i);
            OrderTicketUserCreateDto orderTicketUserCreateDto = new OrderTicketUserCreateDto();
            orderTicketUserCreateDto.setOrderNumber(orderCreateDto.getOrderNumber());
            orderTicketUserCreateDto.setProgramId(programOrderCreateDto.getProgramId());
            orderTicketUserCreateDto.setUserId(programOrderCreateDto.getUserId());
            orderTicketUserCreateDto.setTicketUserId(ticketUserId);
            SeatVo seatVo =
                    Optional.ofNullable(purchaseSeatList.get(i))
                            .orElseThrow(() -> new DaMaiFrameException(BaseCode.SEAT_NOT_EXIST));
            orderTicketUserCreateDto.setSeatId(seatVo.getId());
            orderTicketUserCreateDto.setSeatInfo(seatVo.getRowCode() + "排" + seatVo.getColCode() + "列");
            orderTicketUserCreateDto.setTicketCategoryId(seatVo.getTicketCategoryId());
            orderTicketUserCreateDto.setOrderPrice(seatVo.getPrice());
            orderTicketUserCreateDto.setCreateOrderTime(DateUtils.now());
            orderTicketUserCreateDtoList.add(orderTicketUserCreateDto);
        }

        orderCreateDto.setOrderTicketUserCreateDtoList(orderTicketUserCreateDtoList);

        return orderCreateDto;
    }

    private void updateProgramCacheDataResolution(Long programId, List<SeatVo> seatVoList, OrderStatus orderStatus) {
        // 入参seatVoList是要进行锁定的座位
        if(!(Objects.equals(orderStatus.getCode(), OrderStatus.NO_PAY.getCode())
        || Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode()))) {
            throw new DaMaiFrameException(BaseCode.OPERATE_ORDER_STATUS_NOT_PERMIT);
        }
        List<String> keys = new ArrayList<>();
        keys.add("#");  // 没用，占位的

        // 扣减库存的准备工作
        Map<Long, Long> ticketCategoryCountMap =
                seatVoList.stream().collect(Collectors.groupingBy(SeatVo::getTicketCategoryId, Collectors.counting()));
        JSONArray jsonArray = new JSONArray();  // 用于扣减库存需要的参数
        ticketCategoryCountMap.forEach((k, v) -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("programTicketRemainNumberHashKey", RedisKeyBuild.createRedisKey(
                    RedisKeyManage.PROGRAM_TICKET_REMAIN_NUMBER_HASH_RESOLUTION, programId, k).getRelKey());
            jsonObject.put("ticketCategoryId", String.valueOf(k));
            if(Objects.equals(orderStatus.getCode(), OrderStatus.NO_PAY.getCode())) {
                jsonObject.put("count", "-" + v);
            } else if(Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
                jsonObject.put("count", v);
            }
            jsonArray.add(jsonObject);
        });

        // 从nosold中移除要下单的座位，并将这些座位添加到锁定中
        Map<Long, List<SeatVo>> seatVoMap =
                seatVoList.stream().collect(Collectors.groupingBy(SeatVo::getTicketCategoryId));
        JSONArray delSeatIdjsonArray = new JSONArray();  // 用于移除要下单的座位需要的参数
        JSONArray addSeatDatajsonArray = new JSONArray();  // 用于将这些座位添加到锁定中需要的参数
        seatVoMap.forEach((k, v) -> {
            JSONObject delSeatIdjsonObject = new JSONObject();
            JSONObject seatDatajsonObject = new JSONObject();
            String seatHashKeyDel = "";
            String seatHashKeyAdd = "";
            if(Objects.equals(orderStatus.getCode(), OrderStatus.NO_PAY.getCode())) {
                seatHashKeyDel = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH, programId, k).getRelKey();
                seatHashKeyAdd = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, programId, k).getRelKey();
                // 修改sellStatus
                for (SeatVo seatVo : v) {
                    seatVo.setSellStatus(SellStatus.LOCK.getCode());
                }
            } else if(Objects.equals(orderStatus.getCode(), OrderStatus.CANCEL.getCode())) {
                // 与未支付状态是相反的
                seatHashKeyDel = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, programId, k).getRelKey();
                seatHashKeyAdd = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH, programId, k).getRelKey();
                // 修改sellStatus
                for (SeatVo seatVo : v) {
                    seatVo.setSellStatus(SellStatus.NO_SOLD.getCode());
                }
            }

            delSeatIdjsonObject.put("seatHashKeyDel", seatHashKeyDel);
            delSeatIdjsonObject.put("seatIdList", v.stream().map(SeatVo::getId).map(String::valueOf).collect(Collectors.toList()));
            delSeatIdjsonArray.add(delSeatIdjsonObject);

            seatDatajsonObject.put("seatHashKeyAdd", seatHashKeyAdd);
            List<String> seatDataList = new ArrayList<>();  // 更新后的座位信息
            for (SeatVo seatVo : v) {
                seatDataList.add(String.valueOf(seatVo.getId()));
                seatDataList.add(JSON.toJSONString(seatVo));
            }
            seatDatajsonObject.put("seatDataList", seatDataList);
            addSeatDatajsonArray.add(seatDatajsonObject);
        });

        String[] data = new String[3];
        data[0] = JSON.toJSONString(jsonArray);
        data[1] = JSON.toJSONString(delSeatIdjsonArray);
        data[2] = JSON.toJSONString(addSeatDatajsonArray);
        programCacheResolutionOperate.programCacheOperate(keys, data);
    }

    // getTicketCategoryList 的作用是验证传入的手动座位中的票档id或者自动选座的票档id是否正确，如果正确的话则将这些传入的票档数据返回
    private List<TicketCategoryVo> getTicketCategoryList(ProgramOrderCreateDto programOrderCreateDto, Date showTime) {
        List<TicketCategoryVo> getTicketCategoryVoList = new ArrayList<>();
        // 查询该节目下的所有票档
        List<TicketCategoryVo> ticketCategoryVoList = ticketCategoryService.selectTicketCategoryListByProgramIdMultipleCache(programOrderCreateDto.getProgramId(), showTime);
        Map<Long, TicketCategoryVo> ticketCategoryVoMap = ticketCategoryVoList.stream()
                .collect(Collectors.toMap(TicketCategoryVo::getId, ticketCategoryVo -> ticketCategoryVo));

        // 获取前端传来的用户选择的票档id
        List<SeatDto> seatDtoList = programOrderCreateDto.getSeatDtoList();

        // 手动选择座位
        if(CollectionUtil.isNotEmpty(seatDtoList)) {
            for (SeatDto seatDto : seatDtoList) {
                TicketCategoryVo ticketCategoryVo = ticketCategoryVoMap.get(seatDto.getId());
                if(Objects.nonNull(ticketCategoryVo)){
                    getTicketCategoryVoList.add(ticketCategoryVo);
                } else{
                    throw new DaMaiFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
                }
            }
        }
        // 自动分配座位
        else{
            TicketCategoryVo ticketCategoryVo = ticketCategoryVoMap.get(programOrderCreateDto.getTicketCategoryId());
            if(Objects.nonNull(ticketCategoryVo)){
                getTicketCategoryVoList.add(ticketCategoryVo);
            } else{
                throw new DaMaiFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST_V2);
            }
        }

        return getTicketCategoryVoList;
    }

}
