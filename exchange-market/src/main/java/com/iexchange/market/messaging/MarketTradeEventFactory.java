package com.iexchange.market.messaging;

import com.lmax.disruptor.EventFactory;

/**
 * 行情事件工厂。
 */
public class MarketTradeEventFactory implements EventFactory<MarketTradeEvent> {

    @Override
    public MarketTradeEvent newInstance() {
        return new MarketTradeEvent();
    }
}
