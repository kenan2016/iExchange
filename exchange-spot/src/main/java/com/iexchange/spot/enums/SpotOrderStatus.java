package com.iexchange.spot.enums;

import lombok.Getter;

/**
 * 现货订单状态。
 */
@Getter
public enum SpotOrderStatus {

    /**
     * 新建。
     */
    NEW("NEW"),
    /**
     * 部分成交。
     */
    PARTIAL_FILLED("PARTIAL_FILLED"),
    /**
     * 全部成交。
     */
    FILLED("FILLED"),
    /**
     * 已撤销。
     */
    CANCELED("CANCELED");

    /**
     * 枚举编码。
     */
    private final String code;

    SpotOrderStatus(String code) {
        this.code = code;
    }


}
