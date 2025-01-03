package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.damai.core.RedisKeyManage;
import com.damai.dto.TicketUserDto;
import com.damai.dto.TicketUserIdDto;
import com.damai.dto.TicketUserListDto;
import com.damai.entity.TicketUser;
import com.damai.entity.User;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.TicketUserMapper;
import com.damai.mapper.UserMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.vo.TicketUserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class TicketUserService {

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private TicketUserMapper ticketUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UidGenerator uidGenerator;

    public List<TicketUserVo> list(TicketUserListDto ticketUserListDto) {
        // 先从缓存中查询
        List<TicketUserVo> ticketUserVoList = redisCache.getValueIsList(
                RedisKeyBuild.createRedisKey(RedisKeyManage.TICKET_USER_LIST,
                        ticketUserListDto.getUserId()), TicketUserVo.class);
        if(CollectionUtil.isNotEmpty(ticketUserVoList)){
            log.info("从redis中查到了购票人信息，无需查询数据库");
            return ticketUserVoList;
        }

        // redis中没有，从数据库查
        LambdaQueryWrapper<TicketUser> ticketUserLambdaQueryWrapper = Wrappers.lambdaQuery(TicketUser.class)
                .eq(TicketUser::getUserId, ticketUserListDto.getUserId());
        List<TicketUser> ticketUsers = ticketUserMapper.selectList(ticketUserLambdaQueryWrapper);
        return BeanUtil.copyToList(ticketUsers, TicketUserVo.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(TicketUserDto ticketUserDto) {
        User user = userMapper.selectById(ticketUserDto.getUserId());
        if(Objects.isNull(user)){
            throw new DaMaiFrameException(BaseCode.USER_EMPTY);
        }

        LambdaQueryWrapper<TicketUser> ticketUserLambdaQueryWrapper = Wrappers.lambdaQuery(TicketUser.class)
                .eq(TicketUser::getUserId, ticketUserDto.getUserId())
                .eq(TicketUser::getIdType, ticketUserDto.getIdType())
                .eq(TicketUser::getIdNumber, ticketUserDto.getIdNumber());
        TicketUser ticketUser = ticketUserMapper.selectOne(ticketUserLambdaQueryWrapper);
        if(Objects.nonNull(ticketUser)){
            throw new DaMaiFrameException(BaseCode.TICKET_USER_EXIST);
        }
        TicketUser addTicketUser = new TicketUser();
        BeanUtil.copyProperties(ticketUserDto, addTicketUser);
        addTicketUser.setId(uidGenerator.getUid());
        ticketUserMapper.insert(addTicketUser);
        // 更新购票人缓存
        delTicketUserVoListCache(String.valueOf(ticketUserDto.getUserId()));
    }

    public void delTicketUserVoListCache(String userId) {
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.TICKET_USER_LIST, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(TicketUserIdDto ticketUserIdDto) {
        TicketUser ticketUser = ticketUserMapper.selectById(ticketUserIdDto.getId());
        if(Objects.isNull(ticketUser)) {
            throw new DaMaiFrameException(BaseCode.TICKET_USER_EMPTY);
        }
        ticketUserMapper.deleteById(ticketUserIdDto.getId());
        delTicketUserVoListCache(String.valueOf(ticketUser.getUserId()));
    }
}
