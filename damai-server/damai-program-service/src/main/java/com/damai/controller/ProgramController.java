package com.damai.controller;

import com.damai.common.ApiResponse;
import com.damai.dto.ProgramListDto;
import com.damai.service.ProgramService;
import com.damai.vo.ProgramHomeVo;
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
}
