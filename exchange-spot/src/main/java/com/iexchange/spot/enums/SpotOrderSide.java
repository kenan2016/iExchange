package com.iexchange.spot.enums;

import lombok.Getter;

/**
 * 现货订单方向。
 */
@Getter
public enum SpotOrderSide {

    /**
     * 买入。
     */
    BUY("BUY"),
    /**
     * 卖出。
     */
    SELL("SELL");

    /**
     * 枚举编码。
     */
    private final String code;

    SpotOrderSide(String code) {
        this.code = code;
    }

    /**
     * 通过字符串解析方向，统一转大写。
     */
    public static SpotOrderSide fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("订单方向不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (SpotOrderSide side : values()) {
            if (side.code.equals(normalized)) {
                return side;
            }
        }
        throw new IllegalArgumentException("不支持的订单方向：" + code);
    }
}
