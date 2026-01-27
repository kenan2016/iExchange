package com.iexchange.spot.enums;

import lombok.Getter;

/**
 * 计划委托状态。
 */
@Getter
public enum SpotPlanOrderStatus {
    /**
     * 新建。
     */
    NEW("NEW"),
    /**
     * 已触发。
     */
    TRIGGERED("TRIGGERED"),
    /**
     * 已撤销。
     */
    CANCELED("CANCELED");

    /**
     * 枚举编码。
     */
    private final String code;

    SpotPlanOrderStatus(String code) {
        this.code = code;
    }
}
