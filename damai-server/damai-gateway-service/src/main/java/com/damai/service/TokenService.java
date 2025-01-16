package com.damai.service;

import com.alibaba.fastjson.JSONObject;
import com.damai.core.RedisKeyManage;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.jwt.TokenUtil;
import com.damai.redis.RedisKeyBuild;
import com.damai.util.StringUtil;
import com.damai.vo.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.damai.redis.RedisCache;

import java.util.Optional;

@Component
public class TokenService {

    @Autowired
    private RedisCache redisCache;

    public String parseToken(String token, String tokenSecret) {
        String userStr = TokenUtil.parseToken(token, tokenSecret);
        if(StringUtil.isNotEmpty(userStr)){
            return JSONObject.parseObject(userStr).getString("userId");
        }
        return null;
    }

    public UserVo getUser(String token, String code, String tokenSecret){
        UserVo userVo = null;
        String userId = parseToken(token, tokenSecret);
        if(StringUtil.isNotEmpty(userId)){
            userVo = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, code, userId), UserVo.class);
        }
//        return Optional.ofNullable(userVo).orElseThrow(() -> new DaMaiFrameException(BaseCode.LOGIN_USER_NOT_EXIST));
        return userVo;
    }
}
