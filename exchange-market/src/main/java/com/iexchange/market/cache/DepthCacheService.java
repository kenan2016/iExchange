package com.iexchange.market.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexchange.market.service.model.DepthSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 深度 Redis 缓存服务。
 */
@Slf4j
@Service
public class DepthCacheService {
    private static final String DEPTH_KEY = "depth:data";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DepthCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 缓存深度快照。
     */
    public void cache(DepthSnapshot snapshot) {
        if (snapshot == null || snapshot.getSymbol() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(snapshot);
            stringRedisTemplate.opsForHash().put(DEPTH_KEY, snapshot.getSymbol(), payload);
        } catch (Exception ex) {
            log.warn("深度缓存失败", ex);
        }
    }

    /**
     * 读取缓存深度。
     */
    public DepthSnapshot get(String symbol) {
        try {
            Object payload = stringRedisTemplate.opsForHash().get(DEPTH_KEY, symbol);
            if (payload == null) {
                return null;
            }
            return objectMapper.readValue(payload.toString(), DepthSnapshot.class);
        } catch (Exception ex) {
            log.warn("深度缓存读取失败", ex);
            return null;
        }
    }
}
