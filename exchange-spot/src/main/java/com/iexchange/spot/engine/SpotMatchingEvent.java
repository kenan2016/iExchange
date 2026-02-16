package com.iexchange.spot.engine;

import com.iexchange.spot.entity.SpotOrderEntity;
import java.util.concurrent.CompletableFuture;
import lombok.Data;

/**
 * Disruptor 撮合事件。
 *
 * 说明：
 * - 事件对象会被 Disruptor 复用
 * - future 用于将处理结果回传给业务线程
 */
@Data
public class SpotMatchingEvent {

    /**
     * 事件类型（撮合/撤单）。
     */
    private SpotMatchingEventType type;

    /**
     * 订单信息（taker 或撤单目标）。
     */
    private SpotOrderEntity order;

    /**
     * 异步回执结果，业务线程通过它等待撮合结果。
     */
    private CompletableFuture<SpotMatchingResult> future;

    /**
     * 事件复用时清理字段，避免脏数据串入下一次撮合。
     */
    public void reset() {
        type = null;
        order = null;
        future = null;
    }
}
