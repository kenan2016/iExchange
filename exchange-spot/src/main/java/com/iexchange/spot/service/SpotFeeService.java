package com.iexchange.spot.service;

import com.iexchange.spot.entity.SpotOrderEntity;
import com.iexchange.spot.entity.SpotTradeEntity;

/**
 * 现货手续费服务。
 */
public interface SpotFeeService {

    /**
     * 根据成交记录落手续费流水。
     */
    void recordTradeFee(SpotTradeEntity trade, SpotOrderEntity buyOrder, SpotOrderEntity sellOrder);
}
