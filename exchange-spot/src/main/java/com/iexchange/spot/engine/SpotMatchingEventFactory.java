package com.iexchange.spot.engine;

import com.lmax.disruptor.EventFactory;

/**
 * 撮合事件工厂。
 */
public class SpotMatchingEventFactory implements EventFactory<SpotMatchingEvent> {

    /**
     * 创建可复用的撮合事件对象，供 Disruptor 循环使用。
     */
    @Override
    public SpotMatchingEvent newInstance() {
        return new SpotMatchingEvent();
    }
}
