package com.damai.service.constant;

import java.util.concurrent.TimeUnit;

public class OrderConstant {

    public static final String DELAY_ORDER_CANCEL_TOPIC ="d_delay_order_cancel_topic";

    public static final String DELAY_OPERATE_PROGRAM_DATA_TOPIC = "d_delay_operate_program_data_topic";

    public static final Long DELAY_OPERATE_PROGRAM_DATA_TIME = 1L;

    public static final TimeUnit DELAY_OPERATE_PROGRAM_DATA_TIME_UNIT = TimeUnit.SECONDS;
}
