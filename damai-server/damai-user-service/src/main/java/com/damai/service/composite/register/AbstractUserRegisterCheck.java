package com.damai.service.composite.register;

import com.damai.dto.UserRegisterDto;
import com.damai.enums.CompositeCheckType;
import com.damai.initialize.impl.composite.AbstractComposite;

public abstract class AbstractUserRegisterCheck extends AbstractComposite<UserRegisterDto> {

    @Override
    public String type(){
        return CompositeCheckType.USER_REGISTER_CHECK.getValue();
    }

}

