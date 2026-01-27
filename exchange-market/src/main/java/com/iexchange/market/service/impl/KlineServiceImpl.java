package com.iexchange.market.service.impl;

import com.iexchange.market.cache.KlineCacheService;
import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.document.KlineDocument;
import com.iexchange.market.repository.KlineRepository;
import com.iexchange.market.service.KlineService;
import com.iexchange.market.service.model.KlineBucket;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * K 线服务实现。
 */
@Service
public class KlineServiceImpl implements KlineService {

    private static final Map<String, Integer> BASE_INTERVAL_SECONDS = Map.of(
        "1m", 60,
        "5m", 300,
        "15m", 900,
        "1h", 3600
    );
    private static final Set<String> SUPPORTED_INTERVALS = Set.of("1m", "5m", "15m", "30m", "1h");
    private static final String AGGREGATE_SOURCE_INTERVAL = "5m";
    private static final String AGGREGATE_TARGET_INTERVAL = "30m";
    private static final String AGGREGATE_MODE_MONGO = "mongo";
    private static final String AGGREGATE_MODE_REDIS_FIRST = "redis-first";

    private final Map<String, KlineBucket> bucketStore = new ConcurrentHashMap<>();
    private final KlineRepository repository;
    private final KlineCacheService cacheService;
    private final String aggregateMode;

    public KlineServiceImpl(KlineRepository repository,
                            KlineCacheService cacheService,
                            @Value("${market.kline.aggregate-mode:redis-first}") String aggregateMode) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.aggregateMode = aggregateMode == null ? AGGREGATE_MODE_REDIS_FIRST : aggregateMode.trim().toLowerCase();
    }

    /**
     * 成交驱动 K 线更新。
     *
     * 说明：
     * - 维护多个基础周期的内存桶（1m/5m/15m/1h）
     * - 每次成交更新桶并落库 + 缓存
     * - 以 5m 为来源尝试合成 30m 周期
     */
    @Override
    public void onTrade(SpotTradeEvent event) {
        long tradeTime = toEpochSeconds(event.getTradeTime());
        for (Map.Entry<String, Integer> entry : BASE_INTERVAL_SECONDS.entrySet()) {
            String interval = entry.getKey();
            int seconds = entry.getValue();
            long startTime = tradeTime - tradeTime % seconds;
            long endTime = startTime + seconds;
            String bucketKey = buildBucketKey(event.getSymbol(), interval);
            bucketStore.compute(bucketKey, (symbolKey, bucket) -> {
                // 不同周期分别维护 K 线桶
                if (bucket == null || bucket.getStartTime() != startTime) {
                    bucket = new KlineBucket(
                        event.getSymbol(),
                        startTime,
                        endTime,
                        event.getPrice(),
                        event.getPrice(),
                        event.getPrice(),
                        event.getPrice(),
                        event.getQuantity());
                } else {
                    bucket.apply(event.getPrice(), event.getQuantity());
                }
                // 写入 MongoDB + Redis 缓存
                KlineDocument document = toDocument(bucket, interval);
                repository.save(document);
                cacheService.cache(interval, document);
                if (AGGREGATE_SOURCE_INTERVAL.equals(interval)) {
                    // 只在 5m 周期上尝试合成 30m
                    aggregateFromLowerInterval(event.getSymbol(), startTime);
                }
                return bucket;
            });
        }
    }

    /**
     * 查询 K 线数据，优先读取 Redis 缓存，未命中再回源 MongoDB。
     */
    @Override
    public List<KlineDocument> query(String symbol, String interval, int limit) {
        if (interval == null || !SUPPORTED_INTERVALS.contains(interval.toLowerCase())) {
            throw new IllegalArgumentException("不支持的周期：" + interval);
        }
        int pageSize = Math.max(1, Math.min(limit, 200));
        String normalized = interval.toLowerCase();
        // 先查 Redis 缓存，不命中再回源 MongoDB
        List<KlineDocument> cached = cacheService.getLatest(symbol, normalized, pageSize);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<KlineDocument> documents = repository.findBySymbolAndIntervalOrderByStartTimeDesc(
            symbol, normalized, PageRequest.of(0, pageSize));
        for (KlineDocument document : documents) {
            cacheService.cache(normalized, document);
        }
        return documents;
    }

    /**
     * 转换成交时间为秒级时间戳。
     */
    private long toEpochSeconds(LocalDateTime tradeTime) {
        LocalDateTime actual = tradeTime == null ? LocalDateTime.now() : tradeTime;
        return actual.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * 将内存桶转换为持久化文档。
     */
    private KlineDocument toDocument(KlineBucket bucket, String interval) {
        KlineDocument document = new KlineDocument();
        document.setId(bucket.getSymbol() + "_" + interval + "_" + bucket.getStartTime());
        document.setSymbol(bucket.getSymbol());
        document.setStartTime(bucket.getStartTime());
        document.setEndTime(bucket.getEndTime());
        document.setInterval(interval);
        document.setOpen(bucket.getOpen());
        document.setHigh(bucket.getHigh());
        document.setLow(bucket.getLow());
        document.setClose(bucket.getClose());
        document.setVolume(bucket.getVolume());
        document.setCreatedAt(LocalDateTime.now());
        return document;
    }

    /**
     * 构建内存桶 key：symbol:interval。
     */
    private String buildBucketKey(String symbol, String interval) {
        return symbol + ":" + interval;
    }

    /**
     * 使用 6 根 5m 合成 30m K 线。
     */
    private void aggregateFromLowerInterval(String symbol, long lowerStartTime) {
        Integer lowerSeconds = BASE_INTERVAL_SECONDS.get(AGGREGATE_SOURCE_INTERVAL);
        if (lowerSeconds == null) {
            return;
        }
        int targetSeconds = lowerSeconds * 6;
        long targetStartTime = lowerStartTime - lowerStartTime % targetSeconds;
        long targetEndTime = targetStartTime + targetSeconds;
        if (AGGREGATE_MODE_MONGO.equals(aggregateMode)) {
            aggregateFromLowerIntervalByMongo(symbol, targetStartTime, targetEndTime);
        } else {
            aggregateFromLowerIntervalPreferRedis(symbol, targetStartTime, targetEndTime);
        }
    }

    /**
     * 版本一：直接查 MongoDB 聚合。
     */
    private void aggregateFromLowerIntervalByMongo(String symbol, long targetStartTime, long targetEndTime) {
        String targetId = symbol + "_" + AGGREGATE_TARGET_INTERVAL + "_" + targetStartTime;
        if (repository.existsById(targetId)) {
            return;
        }
        List<KlineDocument> segments = repository.findBySymbolAndIntervalAndStartTimeBetweenOrderByStartTimeAsc(
            symbol, AGGREGATE_SOURCE_INTERVAL, targetStartTime, targetEndTime - 1);
        if (segments.size() != 6) {
            return;
        }
        saveAggregatedKline(symbol, targetStartTime, targetEndTime, segments);
    }

    /**
     * 版本二：优先用 Redis 缓存聚合，不足则回源 MongoDB。
     */
    private void aggregateFromLowerIntervalPreferRedis(String symbol, long targetStartTime, long targetEndTime) {
        String targetId = symbol + "_" + AGGREGATE_TARGET_INTERVAL + "_" + targetStartTime;
        if (repository.existsById(targetId)) {
            return;
        }
        List<KlineDocument> segments = cacheService.getRangeByTime(
            symbol, AGGREGATE_SOURCE_INTERVAL, targetStartTime, targetEndTime - 1);
        if (segments.size() != 6) {
            segments = repository.findBySymbolAndIntervalAndStartTimeBetweenOrderByStartTimeAsc(
                symbol, AGGREGATE_SOURCE_INTERVAL, targetStartTime, targetEndTime - 1);
        }
        if (segments.size() != 6) {
            return;
        }
        saveAggregatedKline(symbol, targetStartTime, targetEndTime, segments);
    }

    /**
     * 保存合成后的 K 线（MongoDB + Redis）。
     */
    private void saveAggregatedKline(String symbol, long targetStartTime, long targetEndTime,
                                     List<KlineDocument> segments) {
        KlineDocument aggregated = new KlineDocument();
        aggregated.setId(symbol + "_" + AGGREGATE_TARGET_INTERVAL + "_" + targetStartTime);
        aggregated.setSymbol(symbol);
        aggregated.setInterval(AGGREGATE_TARGET_INTERVAL);
        aggregated.setStartTime(targetStartTime);
        aggregated.setEndTime(targetEndTime);
        aggregated.setOpen(segments.get(0).getOpen());
        aggregated.setClose(segments.get(segments.size() - 1).getClose());
        aggregated.setHigh(maxHigh(segments));
        aggregated.setLow(minLow(segments));
        aggregated.setVolume(sumVolume(segments));
        aggregated.setCreatedAt(LocalDateTime.now());
        repository.save(aggregated);
        cacheService.cache(AGGREGATE_TARGET_INTERVAL, aggregated);
    }

    /**
     * 合成区间最高价。
     */
    private BigDecimal maxHigh(List<KlineDocument> segments) {
        BigDecimal max = segments.get(0).getHigh();
        for (KlineDocument document : segments) {
            if (document.getHigh().compareTo(max) > 0) {
                max = document.getHigh();
            }
        }
        return max;
    }

    /**
     * 合成区间最低价。
     */
    private BigDecimal minLow(List<KlineDocument> segments) {
        BigDecimal min = segments.get(0).getLow();
        for (KlineDocument document : segments) {
            if (document.getLow().compareTo(min) < 0) {
                min = document.getLow();
            }
        }
        return min;
    }

    /**
     * 合成区间成交量合计。
     */
    private BigDecimal sumVolume(List<KlineDocument> segments) {
        BigDecimal total = BigDecimal.ZERO;
        for (KlineDocument document : segments) {
            total = total.add(document.getVolume());
        }
        return total;
    }
}
