package com.damai.service.tool;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class TokenExpireManager {

    @Value("${token.expire.time:40}")
    private Long tokenExpireTime;
}
