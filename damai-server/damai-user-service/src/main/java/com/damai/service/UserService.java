package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.client.BaseDataClient;
import com.damai.common.ApiResponse;
import com.damai.core.RedisKeyManage;
import com.damai.dto.*;
import com.damai.entity.User;
import com.damai.entity.UserEmail;
import com.damai.entity.UserMobile;
import com.damai.enums.BaseCode;
import com.damai.enums.CompositeCheckType;
import com.damai.exception.DaMaiFrameException;
import com.damai.handler.BloomFilterHandler;
import com.damai.initialize.impl.composite.CompositeContainer;
import com.damai.jwt.TokenUtil;
import com.damai.mapper.UserEmailMapper;
import com.damai.mapper.UserMapper;
import com.damai.mapper.UserMobileMapper;
import com.damai.redis.RedisCacheImpl;
import com.damai.redis.RedisKeyBuild;
import com.damai.servicelock.LockType;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.util.StringUtil;
import com.damai.vo.GetChannelDataVo;
import com.damai.vo.UserLoginVo;
import com.damai.vo.UserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.damai.core.DistributedLockConstants.REGISTER_USER_LOCK;

@Slf4j
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private UserMobileMapper userMobileMapper;

    @Autowired
    private BloomFilterHandler bloomFilterHandler;

    @Autowired
    private CompositeContainer compositeContainer;

    @Autowired
    private UidGenerator uidGenerator;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisCacheImpl redisCache;

    @Autowired
    private UserEmailMapper userEmailMapper;

    @Value("${token.expire.time:40}")
    private Long tokenExpireTime;

    @Autowired
    private BaseDataClient baseDataClient;

    private static final Integer ERROR_COUNT_THRESHOLD = 5;

    @Transactional(rollbackFor = Exception.class)
    @ServiceLock(lockType = LockType.Write, name = REGISTER_USER_LOCK, keys = {"#userRegisterDto.mobile"})
    public Boolean register(UserRegisterDto userRegisterDto) {
        compositeContainer.execute(CompositeCheckType.USER_REGISTER_CHECK.getValue(), userRegisterDto);
        log.info("注册手机号:{}", userRegisterDto.getMobile());

        // 用户表添加
        User user = new User();
        BeanUtil.copyProperties(userRegisterDto, user);
        user.setId(uidGenerator.getUid());
        userMapper.insert(user);
        // System.out.println("Mobile:" + user.getMobile());

        // 用户手机表添加
        UserMobile userMobile = new UserMobile();
        userMobile.setId(uidGenerator.getUid());
        userMobile.setUserId(user.getId());
        userMobile.setMobile(user.getMobile());
        userMobileMapper.insert(userMobile);
        bloomFilterHandler.add(userMobile.getMobile());
        return true;
    }

    public void doExist(String mobile) {
        boolean contains = bloomFilterHandler.contains(mobile);
        if (contains) {  // 布隆过滤器判断存在，还要去数据库里看一下是否真的存在
            LambdaQueryWrapper<UserMobile> queryWrapper = Wrappers.lambdaQuery(UserMobile.class)
                    .eq(UserMobile::getMobile, mobile);
            UserMobile userMobile = userMobileMapper.selectOne(queryWrapper);
            if(Objects.nonNull(userMobile)){
                throw new DaMaiFrameException(BaseCode.USER_EXIST);
            }
        }
    }

    public UserLoginVo login(UserLoginDto userLoginDto) {
        UserLoginVo userLoginVo = new UserLoginVo();
        String code = userLoginDto.getCode();
        String mobile = userLoginDto.getMobile();
        String email = userLoginDto.getEmail();
        String password = userLoginDto.getPassword();

        if(StringUtil.isEmpty(mobile) && StringUtil.isEmpty(email)){
            throw new DaMaiFrameException(BaseCode.USER_MOBILE_AND_EMAIL_NOT_EXIST);
        }

        Long userId;
        if(StringUtil.isNotEmpty(mobile)){
            String errorCountStr = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_MOBILE_ERROR, mobile), String.class);
            if(StringUtil.isNotEmpty(errorCountStr) && Integer.parseInt(errorCountStr) >= ERROR_COUNT_THRESHOLD){
                throw new DaMaiFrameException(BaseCode.MOBILE_ERROR_COUNT_TOO_MANY);
            }
            LambdaQueryWrapper<UserMobile> queryWrapper = Wrappers.lambdaQuery(UserMobile.class)
                    .eq(UserMobile::getMobile, mobile);
            UserMobile userMobile = userMobileMapper.selectOne(queryWrapper);
            if(Objects.isNull(userMobile)){
                redisCache.incrBy(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_MOBILE_ERROR, mobile), 1);
                redisCache.expire(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_MOBILE_ERROR, mobile), 1, TimeUnit.MINUTES);
                throw new DaMaiFrameException(BaseCode.USER_MOBILE_EMPTY);
            }
            userId = userMobile.getUserId();
        } else{
            String errorCountStr = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_EMAIL_ERROR, email), String.class);
            if(StringUtil.isNotEmpty(errorCountStr) && Integer.parseInt(errorCountStr) > ERROR_COUNT_THRESHOLD){
                throw new DaMaiFrameException(BaseCode.EMAIL_ERROR_COUNT_TOO_MANY);
            }
            LambdaQueryWrapper<UserEmail> queryWrapper = Wrappers.lambdaQuery(UserEmail.class)
                    .eq(UserEmail::getEmail, email);
            UserEmail userEmail = userEmailMapper.selectOne(queryWrapper);
            if(Objects.isNull(userEmail)){
                redisCache.incrBy(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_EMAIL_ERROR, email), 1);
                redisCache.expire(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_EMAIL_ERROR, email), 1, TimeUnit.MINUTES);
            }
            userId = userEmail.getUserId();
        }

        // 根据通过电话号码或邮箱查找到的userId查询User表
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getId, userId).eq(User::getPassword, password);
        User user = userMapper.selectOne(queryWrapper);
        if(Objects.isNull(user)){
            throw new DaMaiFrameException(BaseCode.NAME_PASSWORD_ERROR);
        }
        redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, code, user.getId()), user, tokenExpireTime, TimeUnit.MINUTES);
        userLoginVo.setUserId(userId);
        userLoginVo.setToken(createToken(user.getId(), getChannelDataByCode(code).getTokenSecret()));
        return userLoginVo;
    }

    private String createToken(Long userId, String tokenSecret) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("userId", userId);
        return TokenUtil.createToken(String.valueOf(uidGenerator.getUid()), JSON.toJSONString(map), tokenExpireTime * 60 * 1000, tokenSecret);
    }

    public GetChannelDataVo getChannelDataByCode(String code){
        GetChannelDataVo getChannelDataVo = getChannelDataByRedis(code);
        if(Objects.isNull(getChannelDataVo)){
            getChannelDataVo = getChannelDataByClient(code);
            setChannelDataRedis(code,getChannelDataVo);
        }
        return getChannelDataVo;
    }

    private void setChannelDataRedis(String code,GetChannelDataVo getChannelDataVo){
        redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.CHANNEL_DATA,code),getChannelDataVo);
    }

    private GetChannelDataVo getChannelDataByClient(String code) {
        GetChannelDataByCodeDto getChannelDataByCodeDto = new GetChannelDataByCodeDto();
        getChannelDataByCodeDto.setCode(code);
        System.out.println("Code:" + code);
        ApiResponse<GetChannelDataVo> getChannelDataVoApiResponse = baseDataClient.getByCode(getChannelDataByCodeDto);
        System.out.println(getChannelDataVoApiResponse.getCode());
        if(Objects.equals(getChannelDataVoApiResponse.getCode(), BaseCode.SUCCESS.getCode())){
            return getChannelDataVoApiResponse.getData();
        }
        throw new DaMaiFrameException("没有找到ChannelData");
    }

    private GetChannelDataVo getChannelDataByRedis(String code) {
        return redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.CHANNEL_DATA, code), GetChannelDataVo.class);
    }

    public Boolean logout(UserLogoutDto userLogoutDto) {
        String userStr = TokenUtil.parseToken(userLogoutDto.getToken(), getChannelDataByCode(userLogoutDto.getCode()).getTokenSecret());
        if(StringUtil.isEmpty(userStr)) {
            throw new DaMaiFrameException(BaseCode.USER_EMPTY);
        }
        String userId = JSONObject.parseObject(userStr).getString("userId");
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, userLogoutDto.getCode(), userId));
        return true;
    }

    public UserVo getById(UserIdDto userIdDto) {
        User user = userMapper.selectById(userIdDto.getId());
        if (Objects.isNull(user)) {
            throw new DaMaiFrameException(BaseCode.USER_EMPTY);
        }
        UserVo userVo = new UserVo();
        BeanUtil.copyProperties(user,userVo);
        return userVo;
    }
}
