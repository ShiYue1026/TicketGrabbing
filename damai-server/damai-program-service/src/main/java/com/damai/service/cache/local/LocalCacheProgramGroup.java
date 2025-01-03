package com.damai.service.cache.local;

import com.damai.util.DateUtils;
import com.damai.vo.ProgramGroupVo;
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
public class LocalCacheProgramGroup {

    private Cache<String, ProgramGroupVo> localCache;


    @Value("${maximumSize:10000}")
    private Long maximumSize;

    @PostConstruct
    public void localLockCacheInit(){
        localCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfter(new Expiry<String, ProgramGroupVo>() {
                    @Override
                    public long expireAfterCreate(@NonNull final String key, @NonNull final ProgramGroupVo value,
                                                  final long currentTime) {
                        return TimeUnit.MILLISECONDS.toNanos
                                (DateUtils.countBetweenSecond(DateUtils.now(),value.getRecentShowTime()));
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull final String key, @NonNull final ProgramGroupVo value,
                                                  final long currentTime, @NonNegative final long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NonNull final String key, @NonNull final ProgramGroupVo value,
                                                final long currentTime, @NonNegative final long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    /**
     * Caffeine的get是线程安全的
     * */
    public ProgramGroupVo getCache(String id, Function<String, ProgramGroupVo> function){
        return localCache.get(id,function);
    }

    public void del(String id){
        localCache.invalidate(id);
    }
}
