package com.damai.core;

import com.damai.servicelock.LockType;
import com.damai.servicelock.ServiceLocker;
import com.damai.servicelock.impl.RedissonFairLocker;
import com.damai.servicelock.impl.RedissonReadLocker;
import com.damai.servicelock.impl.RedissonReentrantLocker;
import com.damai.servicelock.impl.RedissonWriteLocker;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.Map;

public class ManageLocker {

    private final Map<LockType, ServiceLocker> cacheLocker = new HashMap<>();

    public ManageLocker(RedissonClient redissonClient) {
        cacheLocker.put(LockType.Reentrant, new RedissonReentrantLocker(redissonClient));
        cacheLocker.put(LockType.Fair, new RedissonFairLocker(redissonClient));
        cacheLocker.put(LockType.Read, new RedissonReadLocker(redissonClient));
        cacheLocker.put(LockType.Write, new RedissonWriteLocker(redissonClient));
    }

    public ServiceLocker getReentrantLocker() {
        return cacheLocker.get(LockType.Reentrant);
    }

    public ServiceLocker getFairLocker() {
        return cacheLocker.get(LockType.Fair);
    }

    public ServiceLocker getReadLocker() {
        return cacheLocker.get(LockType.Read);
    }

    public ServiceLocker getWriteLocker() {
        return cacheLocker.get(LockType.Write);
    }
}
