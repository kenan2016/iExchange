package com.iexchange.spot.engine;

import lombok.Getter;

/**
 * 撮合事件类型（Disruptor 事件分发使用）。
 */
@Getter
public enum SpotMatchingEventType {
    /**
     * 撮合事件。
     */
    MATCH("MATCH"),
    /**
     * 撤单事件。
     */
    CANCEL("CANCEL");

    /**
     * 枚举编码。
     */
    private final String code;

    SpotMatchingEventType(String code) {
        this.code = code;
    }
}
