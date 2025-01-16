package com.damai.service.cache.local;

import com.damai.entity.ProgramCategory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class LocalCacheProgramCategory {

    private Cache<String, ProgramCategory> localCache;

    @PostConstruct
    public void localLockCacheInit(){
        localCache = Caffeine.newBuilder().build();
    }

    public ProgramCategory get(String id, Function<String, ProgramCategory> function){
        return localCache.get(id,function);
    }
}