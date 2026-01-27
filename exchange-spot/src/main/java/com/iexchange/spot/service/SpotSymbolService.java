package com.iexchange.spot.service;

import com.iexchange.spot.entity.SpotSymbolEntity;

/**
 * 交易对服务。
 */
public interface SpotSymbolService {

    SpotSymbolEntity getEnabledSymbol(String symbol);
}
