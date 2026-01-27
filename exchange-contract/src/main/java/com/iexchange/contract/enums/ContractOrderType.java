package com.iexchange.contract.enums;

import lombok.Getter;

/**
 * 合约订单类型。
 */
@Getter
public enum ContractOrderType {

    /**
     * 限价单（指定价格成交，未满足条件会挂单等待）。
     */
    LIMIT("LIMIT"),
    /**
     * 市价单（按当前可成交价格立即成交）。
     */
    MARKET("MARKET");

    /**
     * 枚举编码。
     */
    private final String code;

    ContractOrderType(String code) {
        this.code = code;
    }

    public static ContractOrderType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("订单类型不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (ContractOrderType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的订单类型：" + code);
    }
}
