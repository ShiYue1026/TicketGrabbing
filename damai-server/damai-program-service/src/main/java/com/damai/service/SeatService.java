package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.damai.core.RedisKeyManage;
import com.damai.dto.SeatListDto;
import com.damai.entity.ProgramShowTime;
import com.damai.entity.Seat;
import com.damai.enums.BaseCode;
import com.damai.enums.BusinessStatus;
import com.damai.enums.SeatType;
import com.damai.enums.SellStatus;
import com.damai.exception.DaMaiFrameException;
import com.damai.lua.ProgramSeatCacheData;
import com.damai.mapper.SeatMapper;
import com.damai.redis.RedisCacheImpl;
import com.damai.redis.RedisKeyBuild;
import com.damai.servicelock.LockType;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.util.DateUtils;
import com.damai.util.ServiceLockTool;
import com.damai.vo.ProgramVo;
import com.damai.vo.SeatRelateInfoVo;
import com.damai.vo.SeatVo;
import com.damai.vo.TicketCategoryVo;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.damai.core.DistributedLockConstants.GET_SEAT_LOCK;
import static com.damai.core.DistributedLockConstants.SEAT_LOCK;

@Service
public class SeatService {

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramShowTimeService programShowTimeService;

    @Autowired
    private TicketCategoryService ticketCategoryService;

    @Autowired
    private ProgramSeatCacheData programSeatCacheData;

    @Autowired
    private ServiceLockTool serviceLockTool;

    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private RedisCacheImpl redisCache;

    public SeatRelateInfoVo relateInfo(SeatListDto seatListDto) {
        SeatRelateInfoVo seatRelateInfoVo = new SeatRelateInfoVo();

        // 查询演出时间和基本信息
        ProgramShowTime programShowTime = programShowTimeService.selectProgramShowTimeByProgramIdMultipleCache(seatListDto.getProgramId());
        ProgramVo programVo = programService.getByIdMultipleCache(seatListDto.getProgramId(), programShowTime.getShowTime());
        programVo.setShowTime(programShowTime.getShowTime());
        programVo.setShowDayTime(programShowTime.getShowDayTime());
        programVo.setShowWeekTime(programShowTime.getShowWeekTime());

        // 查询票档信息
        List<TicketCategoryVo> ticketCategoryVoList = ticketCategoryService
                .selectTicketCategoryListByProgramIdMultipleCache(seatListDto.getProgramId(), programShowTime.getShowTime());

        List<SeatVo> seatVos = new ArrayList<>();
        for (TicketCategoryVo ticketCategoryVo : ticketCategoryVoList) {
            seatVos.addAll(selectSeatResolution(seatListDto.getProgramId(), ticketCategoryVo.getId(),
                    DateUtils.countBetweenSecond(DateUtils.now(), programShowTime.getShowTime()), TimeUnit.SECONDS));
        }

        if(programVo.getPermitChooseSeat().equals(BusinessStatus.NO.getCode())){
            throw new DaMaiFrameException(BaseCode.PROGRAM_NOT_ALLOW_CHOOSE_SEAT);
        }

        // 按照不同票档进行分类
        Map<String, List<SeatVo>> seatVoMap =
                seatVos.stream().collect(Collectors.groupingBy(seatVo -> seatVo.getPrice().toString()));

        seatRelateInfoVo.setProgramId(programVo.getId());
        seatRelateInfoVo.setPlace(programVo.getPlace());
        seatRelateInfoVo.setShowTime(programShowTime.getShowTime());
        seatRelateInfoVo.setShowWeekTime(programShowTime.getShowWeekTime());
        seatRelateInfoVo.setPriceList(seatVoMap.keySet().stream().sorted().collect(Collectors.toList()));
        seatRelateInfoVo.setSeatVoMap(seatVoMap);

        return seatRelateInfoVo;
    }

    @ServiceLock(lockType = LockType.Read, name = SEAT_LOCK, keys = {"#programId", "#ticketCategoryId"})
    public List<SeatVo> selectSeatResolution(Long programId, Long ticketCategoryId, Long expireTime, TimeUnit timeUnit) {
        List<SeatVo> seatVoList = getSeatVoListByCacheResolution(programId, ticketCategoryId);
        if (CollectionUtil.isNotEmpty(seatVoList)){
            return seatVoList;
        }
        // 双重检测锁
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, GET_SEAT_LOCK, new String[]{String.valueOf(programId), String.valueOf(ticketCategoryId)});
        lock.lock();
        try{
            seatVoList = getSeatVoListByCacheResolution(programId, ticketCategoryId);
            if(CollectionUtil.isNotEmpty(seatVoList)){
                return seatVoList;
            }
            // 从数据库中查
            LambdaQueryWrapper<Seat> seatLambdaQueryWrapper = Wrappers.lambdaQuery(Seat.class)
                    .eq(Seat::getProgramId, programId)
                    .eq(Seat::getTicketCategoryId, ticketCategoryId);
            List<Seat> seats = seatMapper.selectList(seatLambdaQueryWrapper);

            for (Seat seat : seats) {
                SeatVo seatVo = new SeatVo();
                BeanUtil.copyProperties(seat, seatVo);
                seatVo.setSeatTypeName(SeatType.getMsg(seat.getSeatType()));
                seatVoList.add(seatVo);
            }

            Map<Integer, List<SeatVo>> seatMap = seatVoList.stream().collect(Collectors.groupingBy(SeatVo::getSellStatus));
            List<SeatVo> noSoldSeatVoList = seatMap.get(SellStatus.NO_SOLD.getCode());
            List<SeatVo> lockSeatVoList = seatMap.get(SellStatus.LOCK.getCode());
            List<SeatVo> soldSeatVoList = seatMap.get(SellStatus.SOLD.getCode());

            if(CollectionUtil.isNotEmpty(noSoldSeatVoList)){
                redisCache.putHash(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH, programId, ticketCategoryId),
                        noSoldSeatVoList.stream().collect(Collectors.toMap(s -> String.valueOf(s.getId()), s -> s, (v1, v2) -> v2)),
                        expireTime,
                        timeUnit
                );
            }

            if(CollectionUtil.isNotEmpty(lockSeatVoList)){
                redisCache.putHash(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, programId, ticketCategoryId),
                        lockSeatVoList.stream().collect(Collectors.toMap(s -> String.valueOf(s.getId()), s -> s, (v1, v2) -> v2)),
                        expireTime,
                        timeUnit
                );
            }

            if(CollectionUtil.isNotEmpty(soldSeatVoList)){
                redisCache.putHash(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_SOLD_RESOLUTION_HASH, programId, ticketCategoryId),
                        soldSeatVoList.stream().collect(Collectors.toMap(s -> String.valueOf(s.getId()), s -> s, (v1, v2) -> v2)),
                        expireTime,
                        timeUnit
                );
            }

            seatVoList = seatVoList.stream().sorted(Comparator.comparingInt(SeatVo::getRowCode)
                    .thenComparingInt(SeatVo::getColCode))
                    .collect(Collectors.toList());

            return seatVoList;
        } finally {
            lock.unlock();
        }
    }

    private List<SeatVo> getSeatVoListByCacheResolution(Long programId, Long ticketCategoryId) {
        List<String> keys = new ArrayList<>(4);
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_NO_SOLD_RESOLUTION_HASH, programId, ticketCategoryId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_LOCK_RESOLUTION_HASH, programId, ticketCategoryId).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SEAT_SOLD_RESOLUTION_HASH, programId, ticketCategoryId).getRelKey());
        return programSeatCacheData.getData(keys, new String[]{});
    }
}
