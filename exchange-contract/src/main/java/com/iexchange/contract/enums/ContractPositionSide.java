package com.iexchange.contract.enums;

import lombok.Getter;

/**
 * 持仓方向。
 */
@Getter
public enum ContractPositionSide {

    /**
     * 多头（看涨，价格上涨时盈利）。
     */
    LONG("LONG"),
    /**
     * 空头（看跌，价格下跌时盈利）。
     */
    SHORT("SHORT");

    /**
     * 枚举编码。
     */
    private final String code;

    ContractPositionSide(String code) {
        this.code = code;
    }

    public static ContractPositionSide fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("持仓方向不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (ContractPositionSide side : values()) {
            if (side.code.equals(normalized)) {
                return side;
            }
        }
        throw new IllegalArgumentException("不支持的持仓方向：" + code);
    }
}
