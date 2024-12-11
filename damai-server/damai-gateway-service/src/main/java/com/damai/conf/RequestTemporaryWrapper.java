package com.damai.conf;

import com.damai.common.ApiResponse;
import lombok.Data;

import java.util.Map;

@Data
public class RequestTemporaryWrapper {

    private Map<String,String> map;

    private ApiResponse<?> apiResponse;
}
