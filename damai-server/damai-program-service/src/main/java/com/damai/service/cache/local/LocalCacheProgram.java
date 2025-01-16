package com.damai.service.cache.local;

import com.damai.util.DateUtils;
import com.damai.vo.ProgramVo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.annotation.PostConstruct;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class LocalCacheProgram {

    private Cache<String, ProgramVo> localCache;

    @Value("${maximumSize:10000}")
    private Long maximumSize;

    @PostConstruct
    public void localLockCacheInit(){
        System.out.println("init LocalCache");
        localCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfter(new Expiry<String, ProgramVo>() {
                    @Override
                    public long expireAfterCreate(@NonNull final String key, @NonNull final ProgramVo value,
                                                  final long currentTime) {
                        return TimeUnit.MILLISECONDS.toNanos(DateUtils.countBetweenSecond(DateUtils.now(),value.getShowTime()));
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull final String key, @NonNull final ProgramVo value,
                                                  final long currentTime, @NonNegative final long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NonNull final String key, @NonNull final ProgramVo value,
                                                final long currentTime, @NonNegative final long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    /**
     * Caffeine的get是线程安全的
     * */
    public ProgramVo getCache(String id, Function<String, ProgramVo> function){
        return localCache.get(id,function);
    }

    public ProgramVo getCache(String id) {
        return localCache.getIfPresent(id);
    }

    public void del(String id){
        localCache.invalidate(id);
    }
}