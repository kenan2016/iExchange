package com.iexchange.spot.engine;

import com.iexchange.spot.entity.SpotOrderEntity;
import java.util.concurrent.CompletableFuture;
import lombok.Data;

/**
 * Disruptor 撮合事件。
 */
@Data
public class SpotMatchingEvent {

    /**
     * 事件类型（撮合/撤单）。
     */
    private SpotMatchingEventType type;

    /**
     * 订单信息。
     */
    private SpotOrderEntity order;

    /**
     * 异步回执结果。
     */
    private CompletableFuture<SpotMatchingResult> future;

    /**
     * 事件复用时清理字段。
     */
    public void reset() {
        type = null;
        order = null;
        future = null;
    }
}
