package com.damai.service;

import com.baidu.fsg.uid.UidGenerator;
import com.damai.captcha.model.common.ResponseModel;
import com.damai.captcha.model.vo.CaptchaVO;
import com.damai.core.RedisKeyManage;
import com.damai.lua.CheckNeedCaptchaOperate;
import com.damai.redis.RedisKeyBuild;
import com.damai.vo.CheckNeedCaptchaDataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserCaptchaService {

    // 每秒请求的阈值
    @Value("${verify_captcha_threshold:10}")
    private int verifyCaptchaThreshold;

    // 校验验证码id的过期时间
    @Value("${verify_captcha_id_expire_time:60}")
    private int verifyCaptchaIdExpireTime;

    // 是否强制验证码
    @Value("1")
    private int alwaysVerifyCaptcha;

    @Autowired
    private UidGenerator uidGenerator;

    @Autowired
    private CheckNeedCaptchaOperate checkNeedCaptchaOperate;

    @Autowired
    private CaptchaHandle captchaHandle;


    public CheckNeedCaptchaDataVo checkNeedCaptcha() {
        CheckNeedCaptchaDataVo checkNeedCaptchaDataVo = new CheckNeedCaptchaDataVo();

        long currentTimeMillis = System.currentTimeMillis();
        long id = uidGenerator.getUid();
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.COUNTER_COUNT).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.COUNTER_TIMESTAMP).getRelKey());
        keys.add(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID, id).getRelKey());

        String[] data = new String[4];
        data[0] = String.valueOf(verifyCaptchaThreshold);
        data[1] = String.valueOf(currentTimeMillis);
        data[2] = String.valueOf(verifyCaptchaIdExpireTime);
        data[3] = String.valueOf(alwaysVerifyCaptcha);

        Boolean result = checkNeedCaptchaOperate.checkNeedCaptchaOperate(keys, data);

        checkNeedCaptchaDataVo.setCaptchaId(id);
        checkNeedCaptchaDataVo.setVerifyCaptcha(result);
        return checkNeedCaptchaDataVo;
    }

    public ResponseModel getCaptcha(CaptchaVO captchaVO) {
        return captchaHandle.getCaptcha(captchaVO);
    }

    public ResponseModel verifyCaptcha(final CaptchaVO captchaVO){
        return captchaHandle.checkCaptcha(captchaVO);
    }

}
