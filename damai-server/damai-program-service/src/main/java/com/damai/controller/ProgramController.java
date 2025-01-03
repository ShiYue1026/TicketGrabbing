package com.damai.controller;

import com.damai.common.ApiResponse;
import com.damai.dto.*;
import com.damai.page.PageVo;
import com.damai.service.ProgramService;
import com.damai.vo.ProgramHomeVo;
import com.damai.vo.ProgramListVo;
import com.damai.vo.ProgramVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/program")
@Tag(name = "program", description = "节目")
public class ProgramController {

    @Autowired
    ProgramService programService;

    @Operation(summary = "查询主页列表")
    @PostMapping(value = "/home/list")
    public ApiResponse<List<ProgramHomeVo>> selectHomeList(@Valid @RequestBody ProgramListDto programListDto){
        return ApiResponse.ok(programService.selectHomeList(programListDto));
    }

    @Operation(summary = "查询分页列表")
    @PostMapping(value = "/page")
    public ApiResponse<PageVo<ProgramListVo>> selectPage(@Valid @RequestBody ProgramPageListDto programPageListDto){
        return ApiResponse.ok(programService.selectPage(programPageListDto));
    }

    @Operation(summary = "搜索")
    @PostMapping(value = "/search")
    public ApiResponse<PageVo<ProgramListVo>> search(@Valid @RequestBody ProgramSearchDto programSearchDto) {
        return ApiResponse.ok(programService.search(programSearchDto));
    }

    @Operation(summary = "查询详情（根据id）")
    @PostMapping(value = "/detail")
    public ApiResponse<ProgramVo> getDetail(@Valid @RequestBody ProgramGetDto programGetDto){
        return ApiResponse.ok(programService.detailV2(programGetDto));
    }

    @Operation(summary = "查询推荐列表")
    @PostMapping(value = "/recommend/list")
    public ApiResponse<List<ProgramListVo>> recommendList(@Valid @RequestBody ProgramRecommendListDto programRecommendListDto){
        return ApiResponse.ok(programService.recommendList(programRecommendListDto));
    }

}
