package com.damai.servicelock.factory;

import com.damai.servicelock.LockType;
import com.damai.core.ManageLocker;
import com.damai.servicelock.ServiceLocker;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class ServiceLockFactory {

    private final ManageLocker manageLocker;

    public ServiceLocker getLock(LockType lockType){
        return switch (lockType) {
            case Fair -> manageLocker.getFairLocker();
            case Read -> manageLocker.getReadLocker();
            case Write -> manageLocker.getWriteLocker();
            default -> manageLocker.getReentrantLocker();
        };
    }

}
