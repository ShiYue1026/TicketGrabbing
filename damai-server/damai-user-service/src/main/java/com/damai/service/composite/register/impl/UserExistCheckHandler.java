package com.damai.service.composite.register.impl;


import com.damai.dto.UserRegisterDto;
import com.damai.service.UserService;
import com.damai.service.composite.register.AbstractUserRegisterCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserExistCheckHandler extends AbstractUserRegisterCheck {

    @Autowired
    UserService userService;

    @Override
    protected void execute(UserRegisterDto userRegisterDto) {
        userService.doExist(userRegisterDto.getMobile());
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
        return 2;
    }
}
