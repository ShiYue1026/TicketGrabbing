package com.damai.service.cache.local;

import com.damai.core.RedisKeyManage;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.vo.TicketCategoryVo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.annotation.PostConstruct;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class LocalCacheTicketCategory {

    private Cache<Long, List<TicketCategoryVo>> localCache;

    @Value("${maximumSize:10000}")
    private Long maximumSize;

    @Autowired
    private RedisCache redisCache;

    @PostConstruct
    public void localLockCacheInit(){
        localCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfter(new Expiry<Long, List<TicketCategoryVo>>() {
                    @Override
                    public long expireAfterCreate(@NonNull final Long key, @NonNull final List<TicketCategoryVo> value,
                                                  final long currentTime) {
                        Long expire = redisCache.getExpire(RedisKeyBuild.createRedisKey
                                (RedisKeyManage.PROGRAM_TICKET_CATEGORY_LIST, key), TimeUnit.MILLISECONDS);
                        return TimeUnit.MILLISECONDS.toNanos(expire);
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull final Long key, @NonNull final List<TicketCategoryVo> value,
                                                  final long currentTime, @NonNegative final long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NonNull final Long key, @NonNull final List<TicketCategoryVo> value,
                                                final long currentTime, @NonNegative final long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    /**
     * Caffeine的get是线程安全的
     * */
    public List<TicketCategoryVo> getCache(Long id, Function<Long, List<TicketCategoryVo>> function){
        return localCache.get(id,function);
    }

    public void del(Long id){
        localCache.invalidate(id);
    }
}

