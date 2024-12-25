package com.damai.service.composite.register.impl;

import com.damai.dto.UserRegisterDto;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.service.composite.register.AbstractUserRegisterCheck;
import com.damai.service.tool.RequestCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserRegisterCountCheckHandler extends AbstractUserRegisterCheck {

    /**
     用户注册数量
     **/

    @Autowired
    private RequestCounter requestCounter;


    @Override
    protected void execute(UserRegisterDto userRegisterDto) {
        boolean result = requestCounter.onRequest();
        if (result) {
            throw new DaMaiFrameException(BaseCode.USER_REGISTER_FREQUENCY);
        }
    }

    @Override
    public Integer executeParentOrder() {
        return 1;
    }

    @Override
    public Integer executeTier() {
        return 2;
    }

    @Override
    public Integer executeOrder() {
        return 1;
    }
}
