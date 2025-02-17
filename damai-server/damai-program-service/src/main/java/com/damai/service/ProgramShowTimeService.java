package com.damai.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.core.RedisKeyManage;
import com.damai.entity.Program;
import com.damai.entity.ProgramGroup;
import com.damai.entity.ProgramShowTime;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.ProgramGroupMapper;
import com.damai.mapper.ProgramMapper;
import com.damai.mapper.ProgramShowTimeMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisCacheImpl;
import com.damai.redis.RedisKeyBuild;
import com.damai.service.cache.local.LocalCacheProgramShowTime;
import com.damai.servicelock.LockType;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.util.DateUtils;
import com.damai.util.ServiceLockTool;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.damai.core.DistributedLockConstants.GET_PROGRAM_SHOW_TIME_LOCK;
import static com.damai.core.DistributedLockConstants.PROGRAM_SHOW_TIME_LOCK;

@Slf4j
@Service
public class ProgramShowTimeService extends ServiceImpl<ProgramShowTimeMapper, ProgramShowTime> {

    @Autowired
    private ProgramShowTimeMapper programShowTimeMapper;

    @Autowired
    private ProgramMapper programMapper;

    @Autowired
    private ProgramGroupMapper programGroupMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ServiceLockTool serviceLockTool;

    @Autowired
    private LocalCacheProgramShowTime localCacheProgramShowTime;

    @ServiceLock(lockType= LockType.Read,name = PROGRAM_SHOW_TIME_LOCK,keys = {"#programId"})
    public ProgramShowTime selectProgramShowTimeByProgramId(Long programId) {
        ProgramShowTime programShowTime = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME, programId), ProgramShowTime.class);
        if(Objects.nonNull(programShowTime)) {  // 直接从redis中查到了
            return programShowTime;
        }
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, GET_PROGRAM_SHOW_TIME_LOCK, new String[]{String.valueOf(programId)});
        lock.lock();
        try{
            programShowTime = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME, programId), ProgramShowTime.class);
            if(Objects.isNull(programShowTime)) {
                LambdaQueryWrapper<ProgramShowTime> programShowTimeLambdaQueryWrapper =
                        Wrappers.lambdaQuery(ProgramShowTime.class)
                                .eq(ProgramShowTime::getProgramId, programId);
                programShowTime = Optional.ofNullable(programShowTimeMapper.selectOne(programShowTimeLambdaQueryWrapper))
                        .orElseThrow(() -> new DaMaiFrameException(BaseCode.PROGRAM_SHOW_TIME_NOT_EXIST));
                redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME, programId), programShowTime,
                        DateUtils.countBetweenSecond(DateUtils.now(), programShowTime.getShowTime()), TimeUnit.SECONDS);
            }
            return programShowTime;
        } finally {
            lock.unlock();
        }
    }

    public ProgramShowTime simpleSelectProgramShowTimeByProgramIdMultipleCache(Long programId) {
        ProgramShowTime programShowTime = localCacheProgramShowTime.getCache(
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME, programId).getRelKey()
        );
        if(Objects.nonNull(programShowTime)){
            return programShowTime;
        }
        return redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_SHOW_TIME, programId), ProgramShowTime.class);
    }


    public Set<Long> renewal() {
        Set<Long> programIdSet = new HashSet<>();
        LambdaQueryWrapper<ProgramShowTime> programShowTimeLambdaQueryWrapper =
                Wrappers.lambdaQuery(ProgramShowTime.class).
                        le(ProgramShowTime::getShowTime, DateUtils.addDay(DateUtils.now(), 1));
        List<ProgramShowTime> programShowTimes = programShowTimeMapper.selectList(programShowTimeLambdaQueryWrapper);

        List<ProgramShowTime> newProgramShowTimes = new ArrayList<>(programShowTimes.size());

        for (ProgramShowTime programShowTime : programShowTimes) {
            programIdSet.add(programShowTime.getProgramId());
            Date oldShowTime = programShowTime.getShowTime();
            Date newShowTime = DateUtils.addMonth(oldShowTime, 1);
            Date nowDateTime = DateUtils.now();
            while(newShowTime.before(nowDateTime)){
                newShowTime = DateUtils.addMonth(newShowTime, 1);
            }
            Date newShowDayTime = DateUtils.parseDateTime(DateUtils.formatDate(newShowTime) + " 00:00:00");
            ProgramShowTime updateProgramshowTime = new ProgramShowTime();
            updateProgramshowTime.setShowTime(newShowTime);
            updateProgramshowTime.setShowDayTime(newShowDayTime);
            updateProgramshowTime.setShowWeekTime(DateUtils.getWeekStr(newShowTime));
            LambdaUpdateWrapper<ProgramShowTime> programShowTimeLambdaUpdateWrapper =
                    Wrappers.lambdaUpdate(ProgramShowTime.class)
                            .eq(ProgramShowTime::getProgramId, programShowTime.getProgramId())
                            .eq(ProgramShowTime::getId, programShowTime.getId());
            programShowTimeMapper.update(updateProgramshowTime, programShowTimeLambdaUpdateWrapper);

            ProgramShowTime newProgramShowTime = new ProgramShowTime();
            newProgramShowTime.setProgramId(programShowTime.getProgramId());
            newProgramShowTime.setShowTime(newShowTime);
            newProgramShowTimes.add(newProgramShowTime);
        }
        Map<Long, Date> programGroupMap = new HashMap<>(newProgramShowTimes.size());
        for (ProgramShowTime newProgramShowTime : newProgramShowTimes) {
            Program program = programMapper.selectById(newProgramShowTime.getProgramId());
            if(Objects.isNull(program)){
                continue;
            }
            Long programGroupId = program.getProgramGroupId();
            Date showTime = programGroupMap.get(programGroupId);
            if(Objects.isNull(showTime)){
                programGroupMap.put(programGroupId, newProgramShowTime.getShowTime());
            } else {
                if(DateUtil.compare(newProgramShowTime.getShowTime(), showTime) < 0) {
                    programGroupMap.put(programGroupId, newProgramShowTime.getShowTime());
                }
            }
        }

        if(CollectionUtil.isNotEmpty(programGroupMap)){
            programGroupMap.forEach((k, v) -> {
                ProgramGroup programGroup = new ProgramGroup();
                programGroup.setRecentShowTime(v);

                LambdaUpdateWrapper<ProgramGroup> programGroupLambdaUpdateWrapper =
                        Wrappers.lambdaUpdate(ProgramGroup.class)
                                .eq(ProgramGroup::getId, k);
                programGroupMapper.update(programGroup, programGroupLambdaUpdateWrapper);
            });
        }
        return programIdSet;
    }

    public ProgramShowTime selectProgramShowTimeByProgramIdMultipleCache(Long programId) {
        return localCacheProgramShowTime.getCache(RedisKeyBuild.createRedisKey(
                RedisKeyManage.PROGRAM_SHOW_TIME, programId).getRelKey(),
                key -> selectProgramShowTimeByProgramId(programId)
        );
    }
}
