package com.iexchange.market.service.impl;

import com.iexchange.market.cache.TickerCacheService;
import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.service.MarketTickerService;
import com.iexchange.market.service.model.TickerSnapshot;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 行情 Ticker 服务实现。
 */
@Service
public class MarketTickerServiceImpl implements MarketTickerService {

    private final Map<String, TickerSnapshot> tickerStore = new ConcurrentHashMap<>();
    private final TickerCacheService cacheService;

    public MarketTickerServiceImpl(TickerCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void onTrade(SpotTradeEvent event) {
        if (event.getSymbol() == null) {
            return;
        }
        TickerSnapshot snapshot = tickerStore.compute(event.getSymbol(), (symbol, existing) -> {
            if (existing == null) {
                existing = new TickerSnapshot();
                existing.setSymbol(symbol);
                existing.setVolume(BigDecimal.ZERO);
            }
            existing.setLastPrice(event.getPrice());
            existing.setLastTradeTime(event.getTradeTime());
            existing.setVolume(existing.getVolume().add(event.getQuantity()));
            return existing;
        });
        cacheService.cache(snapshot);
    }

    @Override
    public TickerSnapshot getTicker(String symbol) {
        TickerSnapshot cached = cacheService.get(symbol);
        if (cached != null) {
            return cached;
        }
        return tickerStore.get(symbol);
    }
}
