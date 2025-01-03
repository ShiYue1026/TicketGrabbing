package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.core.RedisKeyManage;
import com.damai.dto.ParentProgramCategoryDto;
import com.damai.dto.ProgramCategoryAddDto;
import com.damai.dto.ProgramCategoryDto;
import com.damai.entity.ProgramCategory;
import com.damai.mapper.ProgramCategoryMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.servicelock.LockType;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.vo.ProgramCategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.damai.core.DistributedLockConstants.PROGRAM_CATEGORY_LOCK;

@Slf4j
@Service
public class ProgramCategoryService extends ServiceImpl<ProgramCategoryMapper, ProgramCategory> {

    @Autowired
    private ProgramCategoryMapper programCategoryMapper;

    @Autowired
    private RedisCache redisCache;

    public List<ProgramCategoryVo> selectAll() {
        List<ProgramCategoryVo> programCategoryVoList = new ArrayList<>();
        List<ProgramCategory> programCategoryList = programCategoryMapper.selectList(new QueryWrapper<>());
        if(CollectionUtils.isNotEmpty(programCategoryList)) {
            programCategoryVoList = BeanUtil.copyToList(programCategoryList, ProgramCategoryVo.class);
        }
        return programCategoryVoList;
    }

    public List<ProgramCategoryVo> selectByType(ProgramCategoryDto programCategoryDto) {
        List<ProgramCategoryVo> programCategoryVoList = new ArrayList<>();
        LambdaQueryWrapper<ProgramCategory> lambdaQueryWrapper = Wrappers.lambdaQuery(ProgramCategory.class)
                .eq(ProgramCategory::getType, programCategoryDto.getType());
        List<ProgramCategory> programCategoryList = programCategoryMapper.selectList(lambdaQueryWrapper);
        if(CollectionUtils.isNotEmpty(programCategoryList)) {
            programCategoryVoList = BeanUtil.copyToList(programCategoryList, ProgramCategoryVo.class);
        }
        return programCategoryVoList;
    }

    public List<ProgramCategoryVo> selectByParentProgramCategoryId(ParentProgramCategoryDto parentProgramCategoryDto) {
        List<ProgramCategoryVo> programCategoryVoList = new ArrayList<>();
        LambdaQueryWrapper<ProgramCategory> lambdaQueryWrapper = Wrappers.lambdaQuery(ProgramCategory.class)
                .eq(ProgramCategory::getParentId, parentProgramCategoryDto.getParentProgramCategoryId());
        List<ProgramCategory> programCategoryList = programCategoryMapper.selectList(lambdaQueryWrapper);
        if(CollectionUtils.isNotEmpty(programCategoryList)) {
            programCategoryVoList = BeanUtil.copyToList(programCategoryList, ProgramCategoryVo.class);
        }
        return programCategoryVoList;
    }

    public void saveBatch(List<ProgramCategoryAddDto> programCategoryAddDtoList) {
        // TODO
    }

    public ProgramCategory getProgramCategory(Long programCategoryId) {
        ProgramCategory programCategory = redisCache.getForHash(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_CATEGORY_HASH), String.valueOf(programCategoryId), ProgramCategory.class);
        if(Objects.isNull(programCategory)){
            // 从数据库查
            LambdaQueryWrapper<ProgramCategory> lambdaQueryWrapper = Wrappers.lambdaQuery(ProgramCategory.class)
                    .eq(ProgramCategory::getId, programCategoryId);
            programCategory = programCategoryMapper.selectOne(lambdaQueryWrapper);
            return programCategory;
        }
        log.info("从redis中查询到了节目类型数据，无需查询数据库");
        return programCategory;
    }

    @ServiceLock(lockType = LockType.Write, name = PROGRAM_CATEGORY_LOCK, keys = {"#all"})
    public void programCategoryRedisDataInit() {
        log.info("初始化节目类型数据到redis中......");
        Map<String, ProgramCategory> programCategoryMap = new HashMap<>(64);
        QueryWrapper<ProgramCategory> lambdaQueryWrapper = Wrappers.emptyWrapper();
        List<ProgramCategory> programCategoryList = programCategoryMapper.selectList(lambdaQueryWrapper);
        if(CollectionUtils.isNotEmpty(programCategoryList)) {
            programCategoryMap = programCategoryList.stream().collect(
                    Collectors.toMap(p -> String.valueOf(p.getId()), p -> p, (v1, v2) -> v2));
            redisCache.putHash(RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM_CATEGORY_HASH), programCategoryMap);
        }
    }
}
