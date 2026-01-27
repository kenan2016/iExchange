package com.iexchange.contract.enums;

import lombok.Getter;

/**
 * 合约订单开平动作。
 */
@Getter
public enum ContractOrderAction {

    /**
     * 开仓（建立新的持仓）。
     */
    OPEN("OPEN"),
    /**
     * 平仓（减少或关闭已有持仓）。
     */
    CLOSE("CLOSE");

    /**
     * 枚举编码。
     */
    private final String code;

    ContractOrderAction(String code) {
        this.code = code;
    }

    public static ContractOrderAction fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("开平动作不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (ContractOrderAction action : values()) {
            if (action.code.equals(normalized)) {
                return action;
            }
        }
        throw new IllegalArgumentException("不支持的开平动作：" + code);
    }
}
