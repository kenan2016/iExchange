package com.iexchange.market.service;

import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.service.model.DepthSnapshot;

/**
 * 行情深度服务。
 */
public interface DepthService {

    void onTrade(SpotTradeEvent event);

    DepthSnapshot getDepth(String symbol, int limit);
}
