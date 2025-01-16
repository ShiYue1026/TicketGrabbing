package com.damai.common;

import com.damai.enums.BaseCode;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Data
@Schema(title="ApiResponse", description="数据响应规范结构")
public class ApiResponse<T> implements Serializable {

    @Schema(name="code", type="Integer", description="响应码 0:成功 其余:失败")
    private Integer code;

    @Schema(name="message", type="String", description="错误信息")
    private String msg;

    @Schema(name="data", description="响应的具体数据")
    private T data;

    private ApiResponse() {}

    public static <T> ApiResponse<T> error(Integer code, String msg) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.code = code;
        apiResponse.msg = msg;
        return apiResponse;
    }

    public static <T> ApiResponse<T> error(String msg) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.code = -100;
        apiResponse.msg = msg;
        return apiResponse;
    }

    public static <T> ApiResponse<T> error(Integer code, T data) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.code = code;
        apiResponse.data = data;
        return apiResponse;
    }

    public static <T> ApiResponse<T> error(BaseCode baseCode) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.code = baseCode.getCode();
        apiResponse.msg = baseCode.getMsg();
        return apiResponse;
    }

    public static <T> ApiResponse<T> error(BaseCode baseCode, T data) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.code = baseCode.getCode();
        apiResponse.msg = baseCode.getMsg();
        apiResponse.data = data;
        return apiResponse;
    }

    public static <T> ApiResponse<T> error() {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.code = -100;
        apiResponse.msg = "系统错误，请稍后重试！";
        return apiResponse;
    }

    public static <T> ApiResponse<T> ok() {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.code = 0;
        return apiResponse;
    }

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> apiResponse = new ApiResponse<T>();
        apiResponse.code = 0;
        apiResponse.setData(data);
        return apiResponse;
    }
}
