package com.damai.exception;

import java.util.List;

public class ArgumentException extends BaseException{

    private Integer code;

    private List<ArgumentError> argumentErrorList;

    public ArgumentException(Integer code, List<ArgumentError> argumentErrorList) {
        this.code = code;
        this.argumentErrorList = argumentErrorList;
    }

    public ArgumentException(String message) { super(message); }

    public ArgumentException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ArgumentException(Throwable cause) { super(cause); }

    public ArgumentException(String message, Throwable cause) { super(message, cause); }

    public ArgumentException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}
