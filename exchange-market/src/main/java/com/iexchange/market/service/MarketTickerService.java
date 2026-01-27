package com.iexchange.market.service;

import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.service.model.TickerSnapshot;

/**
 * 行情 Ticker 服务。
 */
public interface MarketTickerService {

    void onTrade(SpotTradeEvent event);

    TickerSnapshot getTicker(String symbol);
}
