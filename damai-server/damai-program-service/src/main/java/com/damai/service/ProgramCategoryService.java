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
import com.damai.vo.ProgramCategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            // TODO
//            Map<String, ProgramCategory> programCategoryMap = programCategoryRedisDataInit();
//            return programCategoryMap.get(String.valueOf(programCategoryId));
            LambdaQueryWrapper<ProgramCategory> lambdaQueryWrapper = Wrappers.lambdaQuery(ProgramCategory.class)
                    .eq(ProgramCategory::getId, programCategoryId);
            programCategory = programCategoryMapper.selectOne(lambdaQueryWrapper);
            return programCategory;
        }
        return programCategory;
    }
}
