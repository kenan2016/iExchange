package com.iexchange.spot.enums;

import lombok.Getter;

/**
 * 现货订单类型。
 */
@Getter
public enum SpotOrderType {

    /**
     * 限价单。
     */
    LIMIT("LIMIT"),
    /**
     * 市价单。
     */
    MARKET("MARKET");

    /**
     * 枚举编码。
     */
    private final String code;

    SpotOrderType(String code) {
        this.code = code;
    }

    /**
     * 通过字符串解析类型，统一转大写。
     */
    public static SpotOrderType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("订单类型不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (SpotOrderType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的订单类型：" + code);
    }
}
