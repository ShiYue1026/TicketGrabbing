package com.damai.servicelock.factory;

import com.damai.servicelock.LockType;
import com.damai.core.ManageLocker;
import com.damai.servicelock.ServiceLocker;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class ServiceLockFactory {

    private final ManageLocker manageLocker;

    public ServiceLocker getLock(LockType lockType){
        ServiceLocker lock = switch (lockType) {
            case Reentrant -> manageLocker.getReentrantLocker();
            case Fair -> manageLocker.getFairLocker();
            case Read -> manageLocker.getReadLocker();
            case Write -> manageLocker.getWriteLocker();
        };
        return lock;
    }

}
