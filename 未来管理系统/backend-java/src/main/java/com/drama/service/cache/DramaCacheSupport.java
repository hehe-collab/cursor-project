package com.drama.service.cache;

import com.drama.entity.Drama;
import com.drama.mapper.DramaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DramaCacheSupport {

    private final DramaMapper dramaMapper;

    @Cacheable(value = "dramas", key = "#id", unless = "#result == null")
    public Drama fetchWithCategory(int id) {
        return dramaMapper.selectByIdWithCategory(id);
    }

    @CacheEvict(value = "dramas", key = "#id")
    public void evictById(int id) {
        // no-op：由 AOP 清理缓存
    }
}
