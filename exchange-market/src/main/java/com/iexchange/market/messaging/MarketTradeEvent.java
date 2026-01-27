package com.iexchange.market.messaging;

import com.iexchange.market.dto.SpotTradeEvent;
import lombok.Data;

/**
 * Disruptor 行情事件载体。
 */
@Data
public class MarketTradeEvent {

    /**
     * 成交事件数据。
     */
    private SpotTradeEvent tradeEvent;

    /**
     * 清理引用，避免长时间持有对象。
     */
    public void clear() {
        this.tradeEvent = null;
    }
}
