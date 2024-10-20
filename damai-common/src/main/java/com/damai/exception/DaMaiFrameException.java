package com.damai.exception;

import lombok.Data;

@Data
public class DaMaiFrameException extends BaseException {

    private Integer code;

    private String msg;

    public DaMaiFrameException() {super();}

    public DaMaiFrameException(String msg) {super(msg);}

    public DaMaiFrameException(String code, String msg) {
        super(msg);
        this.code = Integer.parseInt(code);
        this.msg = msg;
    }

    public DaMaiFrameException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }


}
