package com.damai.util;

import com.damai.constant.LockInfoType;
import com.damai.lockinfo.LockInfoHandle;
import com.damai.lockinfo.factory.LockInfoHandleFactory;
import com.damai.servicelock.LockType;
import com.damai.servicelock.ServiceLocker;
import com.damai.servicelock.factory.ServiceLockFactory;
import lombok.AllArgsConstructor;
import org.redisson.api.RLock;

import java.lang.management.LockInfo;

@AllArgsConstructor
public class ServiceLockTool {

    private final LockInfoHandleFactory lockInfoHandleFactory;

    private final ServiceLockFactory serviceLockFactory;

    public RLock getLock(LockType lockType, String name, String[] keys) {
        LockInfoHandle lockInfoHandle = lockInfoHandleFactory.getLockInfoHandle(LockInfoType.SERVICE_LOCK);
        String lockName = lockInfoHandle.simpleGetLockName(name, keys);
        ServiceLocker lock = serviceLockFactory.getLock(lockType);
        return lock.getLock(lockName);
    }

    public RLock getLock(LockType lockType, String lockName) {
        ServiceLocker lock = serviceLockFactory.getLock(lockType);
        return lock.getLock(lockName);
    }

}
