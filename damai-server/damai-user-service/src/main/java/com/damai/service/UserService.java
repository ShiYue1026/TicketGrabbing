package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.damai.dto.UserRegisterDto;
import com.damai.entity.User;
import com.damai.entity.UserMobile;
import com.damai.enums.BaseCode;
import com.damai.enums.CompositeCheckType;
import com.damai.exception.DaMaiFrameException;
import com.damai.handler.BloomFilterHandler;
import com.damai.initialize.impl.composite.CompositeContainer;
import com.damai.mapper.UserMapper;
import com.damai.mapper.UserMobileMapper;
import com.damai.servicelock.LockType;
import com.damai.servicelock.annotation.ServiceLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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
}
