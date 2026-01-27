package com.iexchange.market.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexchange.market.service.model.TickerSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Ticker Redis 缓存服务。
 */
@Slf4j
@Service
public class TickerCacheService {
    private static final String TICKER_KEY = "ticker:data";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public TickerCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 缓存最新 Ticker。
     */
    public void cache(TickerSnapshot snapshot) {
        if (snapshot == null || snapshot.getSymbol() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(snapshot);
            stringRedisTemplate.opsForHash().put(TICKER_KEY, snapshot.getSymbol(), payload);
        } catch (Exception ex) {
            log.warn("Ticker 缓存失败", ex);
        }
    }

    /**
     * 读取缓存中的 Ticker。
     */
    public TickerSnapshot get(String symbol) {
        try {
            Object payload = stringRedisTemplate.opsForHash().get(TICKER_KEY, symbol);
            if (payload == null) {
                return null;
            }
            return objectMapper.readValue(payload.toString(), TickerSnapshot.class);
        } catch (Exception ex) {
            log.warn("Ticker 缓存读取失败", ex);
            return null;
        }
    }
}
