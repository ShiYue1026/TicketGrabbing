package com.damai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BasePageDto {

    @NotNull
    private Integer pageNumber;

    @NotNull
    private Integer pageSize;
}
