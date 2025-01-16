package com.damai.enums;

public enum OrderStatus {
    /**
     * 订单状态
     * */
    NO_PAY(1,"未支付"),
    CANCEL(2,"已取消"),
    PAY(3,"已支付"),
    REFUND(4,"已退单"),
    ;

    private Integer code;

    private String msg;

    OrderStatus(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static String getMsg(Integer code) {
        for (OrderStatus re : OrderStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }

    public static OrderStatus getRc(Integer code) {
        for (OrderStatus re : OrderStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}