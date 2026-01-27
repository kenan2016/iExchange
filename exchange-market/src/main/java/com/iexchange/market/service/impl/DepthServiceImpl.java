package com.iexchange.market.service.impl;

import com.iexchange.market.cache.DepthCacheService;
import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.service.DepthService;
import com.iexchange.market.service.model.DepthLevel;
import com.iexchange.market.service.model.DepthSnapshot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 行情深度服务实现。
 */
@Service
public class DepthServiceImpl implements DepthService {

    private final Map<String, DepthBook> depthStore = new ConcurrentHashMap<>();
    private final DepthCacheService cacheService;
    private final int maxLevels;

    public DepthServiceImpl(DepthCacheService cacheService,
                            @Value("${market.depth.levels:20}") int maxLevels) {
        this.cacheService = cacheService;
        this.maxLevels = maxLevels;
    }

    @Override
    public void onTrade(SpotTradeEvent event) {
        String symbol = event.getSymbol();
        if (symbol == null) {
            return;
        }
        // 简化：用成交增量去更新深度，不维护全量订单簿
        DepthBook book = depthStore.computeIfAbsent(symbol, key -> new DepthBook(symbol, maxLevels));
        synchronized (book) {
            book.applyTrade(event);
            DepthSnapshot snapshot = book.buildSnapshot(maxLevels);
            cacheService.cache(snapshot);
        }
    }

    @Override
    public DepthSnapshot getDepth(String symbol, int limit) {
        if (symbol == null) {
            return null;
        }
        int size = Math.max(1, Math.min(limit, maxLevels));
        DepthBook book = depthStore.get(symbol);
        if (book != null) {
            synchronized (book) {
                return book.buildSnapshot(size);
            }
        }
        DepthSnapshot cached = cacheService.get(symbol);
        if (cached == null) {
            return null;
        }
        return trimSnapshot(cached, size);
    }

    private DepthSnapshot trimSnapshot(DepthSnapshot snapshot, int limit) {
        DepthSnapshot trimmed = new DepthSnapshot();
        trimmed.setSymbol(snapshot.getSymbol());
        trimmed.setUpdateTime(snapshot.getUpdateTime());
        trimmed.setBids(trimLevels(snapshot.getBids(), limit));
        trimmed.setAsks(trimLevels(snapshot.getAsks(), limit));
        return trimmed;
    }

    private List<DepthLevel> trimLevels(List<DepthLevel> levels, int limit) {
        if (levels == null || levels.isEmpty()) {
            return levels;
        }
        if (levels.size() <= limit) {
            return levels;
        }
        return new ArrayList<>(levels.subList(0, limit));
    }

    /**
     * 深度订单簿（简化）。
     */
    private static class DepthBook {

        private final String symbol;
        private final NavigableMap<BigDecimal, BigDecimal> bids;
        private final NavigableMap<BigDecimal, BigDecimal> asks;
        private final int maxLevels;

        private DepthBook(String symbol, int maxLevels) {
            this.symbol = symbol;
            this.bids = new TreeMap<>((left, right) -> right.compareTo(left));
            this.asks = new TreeMap<>();
            this.maxLevels = maxLevels;
        }

        private void applyTrade(SpotTradeEvent event) {
            BigDecimal price = normalizePrice(event.getPrice());
            BigDecimal quantity = event.getQuantity() == null ? BigDecimal.ZERO : event.getQuantity();
            if (price == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            if ("BUY".equalsIgnoreCase(event.getTakerSide())) {
                merge(asks, price, quantity);
                trim(asks, maxLevels);
            } else if ("SELL".equalsIgnoreCase(event.getTakerSide())) {
                merge(bids, price, quantity);
                trim(bids, maxLevels);
            }
        }

        private DepthSnapshot buildSnapshot(int limit) {
            DepthSnapshot snapshot = new DepthSnapshot();
            snapshot.setSymbol(symbol);
            snapshot.setUpdateTime(LocalDateTime.now());
            snapshot.setBids(buildLevels(bids, limit));
            snapshot.setAsks(buildLevels(asks, limit));
            return snapshot;
        }

        private List<DepthLevel> buildLevels(NavigableMap<BigDecimal, BigDecimal> map, int limit) {
            List<DepthLevel> levels = new ArrayList<>();
            int count = 0;
            for (Map.Entry<BigDecimal, BigDecimal> entry : map.entrySet()) {
                levels.add(new DepthLevel(entry.getKey(), entry.getValue()));
                count++;
                if (count >= limit) {
                    break;
                }
            }
            return levels;
        }

        private void merge(NavigableMap<BigDecimal, BigDecimal> map, BigDecimal price, BigDecimal quantity) {
            map.merge(price, quantity, BigDecimal::add);
        }

        private void trim(NavigableMap<BigDecimal, BigDecimal> map, int limit) {
            while (map.size() > limit) {
                map.pollLastEntry();
            }
        }

        private BigDecimal normalizePrice(BigDecimal price) {
            if (price == null) {
                return null;
            }
            return price.stripTrailingZeros();
        }
    }
}
