package com.damai.pro.limit;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class RateLimiterProperty {

    @Value("${rate.switch:false}")
    private Boolean rateSwitch;

    @Value("${rate.permits:200}")
    private Integer ratePermits;

}
