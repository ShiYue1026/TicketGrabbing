package com.damai.service.composite.register.impl;

import com.damai.captcha.model.common.ResponseModel;
import com.damai.captcha.model.vo.CaptchaVO;
import com.damai.core.RedisKeyManage;
import com.damai.dto.UserRegisterDto;
import com.damai.enums.BaseCode;
import com.damai.enums.VerifyCaptcha;
import com.damai.exception.DaMaiFrameException;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.service.CaptchaHandle;
import com.damai.service.composite.register.AbstractUserRegisterCheck;
import com.damai.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class UserRegisterVerifyCaptcha extends AbstractUserRegisterCheck {

    /**
     验证码二次验证
     **/

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private CaptchaHandle captchaHandle;

    @Override
    protected void execute(UserRegisterDto userRegisterDto) {
        String password = userRegisterDto.getPassword();
        String confirmPassWord = userRegisterDto.getConfirmPassword();
        if(!password.equals(confirmPassWord)){
            throw new DaMaiFrameException(BaseCode.TWO_PASSWORDS_DIFFERENT);
        }

        String verifyCaptcha = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID, userRegisterDto.getCaptchaId()), String.class);
        if(StringUtil.isEmpty(verifyCaptcha)){
            throw new DaMaiFrameException(BaseCode.VERIFY_CAPTCHA_ID_NOT_EXIST);
        }

        if(VerifyCaptcha.YES.getValue().equals(verifyCaptcha)) {
            if(StringUtil.isEmpty(userRegisterDto.getCaptchaVerification())){
                throw new DaMaiFrameException(BaseCode.VERIFY_CAPTCHA_EMPTY);
            }
            log.info("传入的captcha verification:{}", userRegisterDto.getCaptchaVerification());
            CaptchaVO captchaVO = new CaptchaVO();
            captchaVO.setCaptchaVerification(userRegisterDto.getCaptchaVerification());
            ResponseModel responseModel = captchaHandle.verification(captchaVO);
            if (!responseModel.isSuccess()) {
                throw new DaMaiFrameException(responseModel.getRepCode(),responseModel.getRepMsg());
            }
        }
    }

    @Override
    public Integer executeParentOrder() {
        return 0;
    }

    @Override
    public Integer executeTier() {
        return 1;
    }

    @Override
    public Integer executeOrder() {
        return 1;
    }
}
