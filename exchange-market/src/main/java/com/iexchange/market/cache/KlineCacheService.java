package com.iexchange.market.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexchange.market.document.KlineDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * K 线 Redis 缓存服务。
 */
@Slf4j
@Service
public class KlineCacheService {
    private static final String DATA_KEY_PREFIX = "kline:data:";
    private static final String INDEX_KEY_PREFIX = "kline:index:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final int cacheSize;

    public KlineCacheService(StringRedisTemplate stringRedisTemplate,
                             ObjectMapper objectMapper,
                             @Value("${market.kline.cache-size:500}") int cacheSize) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheSize = cacheSize;
    }

    /**
     * 写入 K 线缓存。
     */
    public void cache(String interval, KlineDocument document) {
        try {
            String dataKey = dataKey(document.getSymbol(), interval);
            String indexKey = indexKey(document.getSymbol(), interval);
            String field = String.valueOf(document.getStartTime());
            String payload = objectMapper.writeValueAsString(document);
            stringRedisTemplate.opsForHash().put(dataKey, field, payload);
            stringRedisTemplate.opsForZSet().add(indexKey, field, document.getStartTime());
            trimCache(indexKey, dataKey);
        } catch (JsonProcessingException ex) {
            log.warn("K线缓存序列化失败", ex);
        } catch (Exception ex) {
            log.warn("K线缓存写入失败", ex);
        }
    }

    /**
     * 查询最近 N 条 K 线。
     */
    public List<KlineDocument> getLatest(String symbol, String interval, int limit) {
        List<KlineDocument> result = new ArrayList<>();
        String dataKey = dataKey(symbol, interval);
        String indexKey = indexKey(symbol, interval);
        Set<String> startTimes = stringRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
        if (startTimes == null || startTimes.isEmpty()) {
            return result;
        }
        for (String startTime : startTimes) {
            Object payload = stringRedisTemplate.opsForHash().get(dataKey, startTime);
            if (payload == null) {
                continue;
            }
            try {
                KlineDocument document = objectMapper.readValue(payload.toString(), KlineDocument.class);
                result.add(document);
            } catch (Exception ex) {
                log.warn("K线缓存反序列化失败", ex);
            }
        }
        return result;
    }

    /**
     * 按时间范围获取 K 线（，基于 ZSET 分值）。
     */
    public List<KlineDocument> getRangeByTime(String symbol, String interval, long startTime, long endTime) {
        List<KlineDocument> result = new ArrayList<>();
        String dataKey = dataKey(symbol, interval);
        String indexKey = indexKey(symbol, interval);
        Set<String> startTimes = stringRedisTemplate.opsForZSet().rangeByScore(indexKey, startTime, endTime);
        if (startTimes == null || startTimes.isEmpty()) {
            return result;
        }
        for (String startTimeValue : startTimes) {
            Object payload = stringRedisTemplate.opsForHash().get(dataKey, startTimeValue);
            if (payload == null) {
                continue;
            }
            try {
                KlineDocument document = objectMapper.readValue(payload.toString(), KlineDocument.class);
                result.add(document);
            } catch (Exception ex) {
                log.warn("K线缓存反序列化失败", ex);
            }
        }
        return result;
    }

    /**
     * 控制缓存条数，超过上限则移除最早的 K 线数据。
     */
    private void trimCache(String indexKey, String dataKey) {
        Long size = stringRedisTemplate.opsForZSet().zCard(indexKey);
        if (size == null || size <= cacheSize) {
            return;
        }
        long removeCount = size - cacheSize;
        Set<String> toRemove = stringRedisTemplate.opsForZSet().range(indexKey, 0, removeCount - 1);
        if (toRemove != null && !toRemove.isEmpty()) {
            stringRedisTemplate.opsForZSet().removeRange(indexKey, 0, removeCount - 1);
            stringRedisTemplate.opsForHash().delete(dataKey, toRemove.toArray());
        }
    }

    private String dataKey(String symbol, String interval) {
        return DATA_KEY_PREFIX + symbol + ":" + interval;
    }

    private String indexKey(String symbol, String interval) {
        return INDEX_KEY_PREFIX + symbol + ":" + interval;
    }
}
