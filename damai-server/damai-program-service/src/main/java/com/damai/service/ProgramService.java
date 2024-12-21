package com.damai.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.client.BaseDataClient;
import com.damai.common.ApiResponse;
import com.damai.dto.*;
import com.damai.entity.*;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.ProgramCategoryMapper;
import com.damai.mapper.ProgramMapper;
import com.damai.mapper.ProgramShowTimeMapper;
import com.damai.mapper.TicketCategoryMapper;
import com.damai.page.PageUtil;
import com.damai.page.PageVo;
import com.damai.service.es.ProgramEs;
import com.damai.util.DateUtils;
import com.damai.vo.AreaVo;
import com.damai.vo.ProgramHomeVo;
import com.damai.vo.ProgramListVo;
import com.damai.vo.ProgramVo;
import com.damai.service.constant.ProgramTimeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.core.Collect;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.damai.util.DateUtils.FORMAT_DATE;

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

    @Autowired
    private BaseDataClient baseDataClient;

    @Autowired
    private ProgramCategoryService programCategoryService;

    @Autowired
    ProgramEs programEs;

    /**
     * 查询主页信息
     * @param programListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public List<ProgramHomeVo> selectHomeList(ProgramListDto programListDto) {
        List<ProgramHomeVo> programHomeVoList = programEs.selectHomeList(programListDto);
        if(CollectionUtil.isNotEmpty(programHomeVoList)){
            log.info("从Es中查到了主页列表的节目数据，无需查询数据库");
            return programHomeVoList;
        }
        log.info("需要查询数据库获取主页节目列表");
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


    /**
     * 获取所有当前有效的节目id
     * */
    public List<Long> getAllProgramIdList() {
        LambdaQueryWrapper<Program> programLambdaQueryWrapper = Wrappers.lambdaQuery(Program.class)
                .select(Program::getId);

        List<Program> programList = programMapper.selectList(programLambdaQueryWrapper);

        return programList.stream().map(Program::getId).collect(Collectors.toList());
    }

    /**
     * 从不同的表中查询节目的详细信息，填充ProgramVo的各个属性
     * */
    public ProgramVo getDetailFromDb(Long programId) {
        ProgramVo programVo = createProgramVo(programId);

        ProgramCategory programCategory = getProgramCategory(programVo.getProgramCategoryId());
        if(Objects.nonNull(programCategory)) {
            programVo.setProgramCategoryName(programCategory.getName());
        }
        ProgramCategory parentProgramCategory = getProgramCategory(programVo.getParentProgramCategoryId());
        if(Objects.nonNull(parentProgramCategory)) {
            programVo.setParentProgramCategoryName(parentProgramCategory.getName());
        }

        LambdaQueryWrapper<ProgramShowTime> programShowTimeLambdaQueryWrapper =
                Wrappers.lambdaQuery(ProgramShowTime.class).eq(ProgramShowTime::getProgramId, programId);
        ProgramShowTime programShowTime = Optional.ofNullable(programShowTimeMapper.selectOne(programShowTimeLambdaQueryWrapper))
                .orElseThrow(() -> new DaMaiFrameException(BaseCode.PROGRAM_SHOW_TIME_NOT_EXIST));

        programVo.setShowTime(programShowTime.getShowTime());
        programVo.setShowDayTime(programShowTime.getShowDayTime());
        programVo.setShowWeekTime(programShowTime.getShowWeekTime());

        return programVo;
    }

    /**
     * 用program表中的属性，填充ProgramVo的部分属性
     * */
    private ProgramVo createProgramVo(Long programId){
        ProgramVo programVo = new ProgramVo();
        Program program = Optional.ofNullable(programMapper.selectById(programId))
                .orElseThrow(() -> new DaMaiFrameException(BaseCode.PROGRAM_NOT_EXIST));

        BeanUtils.copyProperties(program, programVo);
        AreaGetDto areaGetDto = new AreaGetDto();
        areaGetDto.setId(program.getAreaId());
        ApiResponse<AreaVo> areaResponse = baseDataClient.getById(areaGetDto);
        if(Objects.equals(areaResponse.getCode(), ApiResponse.ok().getCode())){
            if(Objects.nonNull(areaResponse.getData())){
                programVo.setAreaName(areaResponse.getData().getName());
            }
        }else{
            log.error("base-data rpc getById error areaResponse:{}", JSON.toJSONString(areaResponse));
        }
        return programVo;
    }

    /**
     * 根据类型id查询节目类型表
     * */
    public ProgramCategory getProgramCategory(Long programCategoryId){
        return programCategoryService.getProgramCategory(programCategoryId);
    }

    /**
     * 查询分类列表
     * @param programPageListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    public PageVo<ProgramListVo> selectPage(ProgramPageListDto programPageListDto) {
        setQueryTime(programPageListDto);
        PageVo<ProgramListVo> pageVo = programEs.selectPage(programPageListDto);
        if(CollectionUtil.isNotEmpty(pageVo.getList())) {
            log.info("从Es中查到了分类列表的节目数据，无需查询数据库");
            return pageVo;
        }
        return dbSelectPage(programPageListDto);
    }


    /**
     * 查询分类信息（数据库查询）
     * @param programPageListDto 查询节目数据的入参
     * @return 执行后的结果
     * */
    private PageVo<ProgramListVo> dbSelectPage(ProgramPageListDto programPageListDto) {
        IPage<ProgramJoinShowTime> iPage = programMapper.selectPage(PageUtil.getPageParams(programPageListDto), programPageListDto);
        if(CollectionUtil.isEmpty(iPage.getRecords())) {
            return new PageVo<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal(), new ArrayList<>());
        }

        Set<Long> programCategoryIdSet = iPage.getRecords().stream().map(Program::getProgramCategoryId).collect(Collectors.toSet());
        Map<Long, String> programCategoryMap = selectProgramCategoryMap(programCategoryIdSet);

        List<Long> programIdList = iPage.getRecords().stream().map(Program::getId).collect(Collectors.toList());
        Map<Long, TicketCategoryAggregate> ticketCategorieMap = selectTicketCategoryMap(programIdList);

        Map<Long, String> tempAreaMap = new HashMap<>(64);
        AreaSelectDto areaSelectDto = new AreaSelectDto();
        areaSelectDto.setIdList(iPage.getRecords().stream().map(Program::getAreaId).distinct().collect(Collectors.toList()));
        ApiResponse<List<AreaVo>> areaResponse = baseDataClient.selectByIdList(areaSelectDto);

        if(Objects.equals(areaResponse.getCode(), ApiResponse.ok().getCode())){
            if(CollectionUtil.isNotEmpty(areaResponse.getData())) {
                tempAreaMap = areaResponse.getData()
                        .stream()
                        .collect(Collectors.toMap(AreaVo::getId, AreaVo::getName, (v1, v2) -> v2));
            }
        } else{
            log.error("base-data selectByIdList rpc error areaResponse:{}", com.alibaba.fastjson.JSON.toJSONString(areaResponse));
        }
        Map<Long,String> areaMap = tempAreaMap;

        return PageUtil.convertPage(iPage, programJoinShowTime -> {
           ProgramListVo programListVo = new ProgramListVo();
           BeanUtil.copyProperties(programJoinShowTime, programListVo);

           programListVo.setAreaName(areaMap.get(programJoinShowTime.getAreaId()));
           programListVo.setProgramCategoryName(programCategoryMap.get(programJoinShowTime.getProgramCategoryId()));
            programListVo.setMinPrice(Optional.ofNullable(ticketCategorieMap.get(programJoinShowTime.getId()))
                    .map(TicketCategoryAggregate::getMinPrice).orElse(null));
            programListVo.setMaxPrice(Optional.ofNullable(ticketCategorieMap.get(programJoinShowTime.getId()))
                    .map(TicketCategoryAggregate::getMaxPrice).orElse(null));
           return programListVo;
        });
    }


    /**
     * 根据用户选中的时间类型设置查询的时间范围
     * */
    private void setQueryTime(ProgramPageListDto programPageListDto) {
        switch (programPageListDto.getTimeType()) {
            case ProgramTimeType.TODAY:
                programPageListDto.setStartDateTime(DateUtils.now(FORMAT_DATE));
                programPageListDto.setEndDateTime(DateUtils.now(FORMAT_DATE));
                break;
            case ProgramTimeType.TOMORROW:
                programPageListDto.setStartDateTime(DateUtils.addDay(DateUtils.now(FORMAT_DATE), 1));
                programPageListDto.setEndDateTime(DateUtils.addDay(DateUtils.now(FORMAT_DATE), 1));
                break;
            case ProgramTimeType.WEEK:
                programPageListDto.setStartDateTime(DateUtils.now(FORMAT_DATE));
                programPageListDto.setEndDateTime(DateUtils.addWeek(DateUtils.now(FORMAT_DATE),1));
                break;
            case ProgramTimeType.MONTH:
                programPageListDto.setStartDateTime(DateUtils.now(FORMAT_DATE));
                programPageListDto.setEndDateTime(DateUtils.addMonth(DateUtils.now(FORMAT_DATE),1));
                break;
            case ProgramTimeType.CALENDAR:
                if (Objects.isNull(programPageListDto.getStartDateTime())) {
                    throw new DaMaiFrameException(BaseCode.START_DATE_TIME_NOT_EXIST);
                }
                if (Objects.isNull(programPageListDto.getEndDateTime())) {
                    throw new DaMaiFrameException(BaseCode.END_DATE_TIME_NOT_EXIST);
                }
                break;
            default:
                programPageListDto.setStartDateTime(null);
                programPageListDto.setEndDateTime(null);
        }
    }

    /**
     * 搜索
     * @param programSearchDto 搜索节目数据的入参
     * @return 执行后的结果
     * */
    public PageVo<ProgramListVo> search(ProgramSearchDto programSearchDto) {
        setQueryTime(programSearchDto);
        return programEs.search(programSearchDto);
    }
}
