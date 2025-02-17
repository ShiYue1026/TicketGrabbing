package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.core.RedisKeyManage;
import com.damai.dto.AreaGetDto;
import com.damai.dto.AreaSelectDto;
import com.damai.entity.Area;
import com.damai.enums.AreaType;
import com.damai.enums.BusinessStatus;
import com.damai.mapper.AreaMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.vo.AreaVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.List;

@Slf4j
@Service
public class AreaService extends ServiceImpl<AreaMapper, Area> {

    @Autowired
    AreaMapper areaMapper;

    @Autowired
    RedisCache redisCache;

    public AreaVo current() {
        AreaVo areaVo = new AreaVo();
        LambdaQueryWrapper<Area> wrapper = Wrappers.lambdaQuery(Area.class)
                .eq(Area::getId, 2);
        Area area = areaMapper.selectOne(wrapper);
        if(Objects.nonNull(area)){
            BeanUtil.copyProperties(area, areaVo);
        }
        return areaVo;
    }

    public List<AreaVo> selectCityData() {
        List<AreaVo> areaVos = redisCache.rangeForList(RedisKeyBuild.createRedisKey(RedisKeyManage.AREA_PROVINCE_LIST), 0 , -1, AreaVo.class);
        if(CollectionUtils.isNotEmpty(areaVos)){
            return areaVos;
        }
        LambdaQueryWrapper<Area> lambdaQueryWrapper = Wrappers.lambdaQuery(Area.class)
                .eq(Area::getType, AreaType.MUNICIPALITIES.getCode())
                .or(wrapper -> wrapper
                        .eq(Area::getType, AreaType.PROVINCE.getCode())
                        .eq(Area::getMunicipality, BusinessStatus.YES.getCode()));

        List<Area> areas = areaMapper.selectList(lambdaQueryWrapper);
        areaVos = BeanUtil.copyToList(areas, AreaVo.class);

        if(CollectionUtils.isNotEmpty(areas)){
            // TODO 放入redisCache中
        }
        return areaVos;
    }

    public AreaVo getById(AreaGetDto areaGetDto) {
        AreaVo areaVo = new AreaVo();
        LambdaQueryWrapper<Area> wrapper = Wrappers.lambdaQuery(Area.class)
                .eq(Area::getId, areaGetDto.getId());
        Area area = areaMapper.selectOne(wrapper);
        if(Objects.nonNull(area)){
            BeanUtil.copyProperties(area, areaVo);
        }
        return areaVo;
    }

    public List<AreaVo> hot() {
        final LambdaQueryWrapper<Area> lambdaQueryWrapper = Wrappers.lambdaQuery(Area.class)
                .in(Area::getName, "全国","北京","上海","深圳","广州","杭州","天津","重庆","成都","中国香港");
        List<Area> areas = areaMapper.selectList(lambdaQueryWrapper);
        return BeanUtil.copyToList(areas,AreaVo.class);
    }

    public List<AreaVo> selectByIdList(AreaSelectDto areaSelectDto) {
        final LambdaQueryWrapper<Area> lambdaQueryWrapper = Wrappers.lambdaQuery(Area.class)
                .in(Area::getId, areaSelectDto.getIdList());
        List<Area> areas = areaMapper.selectList(lambdaQueryWrapper);
        return BeanUtil.copyToList(areas,AreaVo.class);
    }
}
