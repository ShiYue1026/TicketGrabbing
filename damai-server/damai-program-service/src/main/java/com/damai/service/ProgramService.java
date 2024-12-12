package com.damai.service;


import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.dto.ProgramListDto;
import com.damai.entity.Program;
import com.damai.entity.ProgramCategory;
import com.damai.entity.ProgramShowTime;
import com.damai.entity.TicketCategoryAggregate;
import com.damai.mapper.ProgramCategoryMapper;
import com.damai.mapper.ProgramMapper;
import com.damai.mapper.ProgramShowTimeMapper;
import com.damai.mapper.TicketCategoryMapper;
import com.damai.vo.ProgramHomeVo;
import com.damai.vo.ProgramListVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProgramService extends ServiceImpl<ProgramMapper, Program> {

    @Autowired
    private ProgramMapper programMapper;

    @Autowired
    private ProgramCategoryMapper programCategoryMapper;

    @Autowired
    private ProgramShowTimeMapper programShowTimeMapper;

    @Autowired
    private TicketCategoryMapper ticketCategoryMapper;

    /**
     * 查询主页信息
     * @param programListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public List<ProgramHomeVo> selectHomeList(ProgramListDto programListDto) {
        return dbSelectHomeList(programListDto);
    }

    /**
     * 查询主页信息（数据库查询）
     * @param programPageListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
     private List<ProgramHomeVo> dbSelectHomeList(ProgramListDto programPageListDto) {
         List<ProgramHomeVo> programHomeVoList = new ArrayList<>();

         // 获取所有父类型id对应的节目类型名称
         Map<Long, String> programCategoryMap = selectProgramCategoryMap(programPageListDto.getParentProgramCategoryIds());

        // 根据area_id和parent_id去表中查找符合条件的节目
         List<Program> programList = programMapper.selectHomeList(programPageListDto);

         System.out.println(programList);

         if (CollectionUtil.isEmpty(programList)) {
             return programHomeVoList;
         }

         // 符合条件的节目的id
         List<Long> programIdList = programList.stream().map(Program::getId).collect(Collectors.toList());

         // 根据节目id获取演出时间信息
         LambdaQueryWrapper<ProgramShowTime> psLambdaQueryWrapper = Wrappers.lambdaQuery(ProgramShowTime.class)
                 .in(ProgramShowTime::getProgramId, programIdList);
         List<ProgramShowTime> programShowTimeList = programShowTimeMapper.selectList(psLambdaQueryWrapper);

         // 将List转为map<节目id,ProgramShowTime>的格式
         Map<Long, List<ProgramShowTime>> programShowTimeMap = programShowTimeList.stream().collect(Collectors.groupingBy(ProgramShowTime::getId));

         // 根据节目id获取价格信息（每个节目的最高票价和最低票价）
         Map<Long, TicketCategoryAggregate> ticketCategoryMap = selectTicketCategoryMap(programIdList);

         // 将符合条件的节目按父类型id分组
         Map<Long, List<Program>> programMap = programList.stream().collect(Collectors.groupingBy(Program::getParentProgramCategoryId));

         for (Map.Entry<Long, List<Program>> programEntry : programMap.entrySet()) {
             Long key = programEntry.getKey();  // 每个父类型id
             List<Program> value = programEntry.getValue();  // 每个父类型id下的7个节目
             List<ProgramListVo> programListVoList = new ArrayList<>();
             for (Program program : value) {
                 ProgramListVo programListVo = new ProgramListVo();
                 BeanUtils.copyProperties(program, programListVo);

                 programListVo.setShowTime(Optional.ofNullable(programShowTimeMap.get(program.getId()))
                         .filter(list -> !list.isEmpty())
                         .map(list -> list.get(0))
                         .map(ProgramShowTime::getShowTime)
                         .orElse(null));
                 programListVo.setShowDayTime(Optional.ofNullable(programShowTimeMap.get(program.getId()))
                         .filter(list -> !list.isEmpty())
                         .map(list -> list.get(0))
                         .map(ProgramShowTime::getShowDayTime)
                         .orElse(null));
                 programListVo.setShowWeekTime(Optional.ofNullable(programShowTimeMap.get(program.getId()))
                         .filter(list -> !list.isEmpty())
                         .map(list -> list.get(0))
                         .map(ProgramShowTime::getShowWeekTime)
                         .orElse(null));

                 programListVo.setMaxPrice(Optional.ofNullable(ticketCategoryMap.get(program.getId()))
                         .map(TicketCategoryAggregate::getMaxPrice).orElse(null));
                 programListVo.setMinPrice(Optional.ofNullable(ticketCategoryMap.get(program.getId()))
                         .map(TicketCategoryAggregate::getMinPrice).orElse(null));
                 programListVoList.add(programListVo);
             }
             ProgramHomeVo programHomeVo = new ProgramHomeVo();
             programHomeVo.setCategoryName(programCategoryMap.get(key));
             programHomeVo.setCategoryId(key);
             programHomeVo.setProgramListVoList(programListVoList);
             programHomeVoList.add(programHomeVo);
         }
         return programHomeVoList;
     }

    /**
     * 根据传过来的id获取对应每个节目的最高票价和最低票价，返回Map
     * */
    public Map<Long, TicketCategoryAggregate> selectTicketCategoryMap(List<Long> programIdList){
        List<TicketCategoryAggregate> ticketCategoryAggregateList = ticketCategoryMapper.selectAggregateList(programIdList);
        return ticketCategoryAggregateList.stream().collect(Collectors.toMap(TicketCategoryAggregate::getProgramId,
                ticketCategory -> ticketCategory, (v1, v2) -> v2));
    }

    /**
     * 根据传过来的id获取对应的节目类型，返回Map
     * */
     public Map<Long, String> selectProgramCategoryMap(Collection<Long> programCategoryIdList){
         // 构造条件
         LambdaQueryWrapper<ProgramCategory> pcLambdaQueryWrapper = Wrappers.lambdaQuery(ProgramCategory.class)
                 .in(ProgramCategory::getId, programCategoryIdList);
         // 进行查询
         List<ProgramCategory> programCategoryList = programCategoryMapper.selectList(pcLambdaQueryWrapper);

         // 返回结果
         return programCategoryList.stream().collect(Collectors.toMap(ProgramCategory::getId, ProgramCategory::getName, (v1, v2) -> v2));
     }
}
