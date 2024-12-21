package com.damai;

import com.damai.base.BaseThreadPool;
import com.damai.namefactory.BusinessNameThreadFactory;
import com.damai.rejectedexecutionhandler.ThreadPoolRejectedExecutionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.*;

public class BusinessThreadPool extends BaseThreadPool {
    private static ThreadPoolExecutor execute = null;

    static {
        execute = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() + 1,
                maximumPoolSize(),
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(600),
                new BusinessNameThreadFactory(),
                new ThreadPoolRejectedExecutionHandler.BusinessAbortPolicy()
        );
    }

    private static Integer maximumPoolSize() {
        return new BigDecimal(Runtime.getRuntime().availableProcessors())
                .divide(new BigDecimal("0.2"), 0, RoundingMode.HALF_UP).intValue();
    }

    public static void execute(Runnable r) {
        execute.execute(wrapTask(r, getContextForTask(), getContextForHold()));
    }

    public static <T> Future<T> submit(Callable<T> c) {
        return execute.submit(wrapTask(c, getContextForTask(), getContextForHold()));
    }
}
