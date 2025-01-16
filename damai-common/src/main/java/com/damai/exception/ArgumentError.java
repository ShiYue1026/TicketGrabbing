package com.damai.exception;

import lombok.Data;

@Data
public class ArgumentError {

    private String argumentName;

    private String msg;
}
