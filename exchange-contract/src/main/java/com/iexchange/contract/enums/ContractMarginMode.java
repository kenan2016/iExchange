package com.iexchange.contract.enums;

import lombok.Getter;

/**
 * 保证金模式。
 */
@Getter
public enum ContractMarginMode {

    /**
     * 全仓（所有仓位共用保证金账户余额）。
     *
     * 说明：
     * - 账户可用余额会共同承担风险
     * - 某个仓位亏损会影响其他仓位的可用保证金
     */
    CROSS("CROSS"),
    /**
     * 逐仓（每个仓位独立占用保证金）。
     *
     * 说明：
     * - 亏损只影响当前仓位的保证金
     * - 其他仓位不被牵连，但也无法共享余额
     */
    ISOLATED("ISOLATED");

    /**
     * 枚举编码。
     */
    private final String code;

    ContractMarginMode(String code) {
        this.code = code;
    }

    public static ContractMarginMode fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("保证金模式不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (ContractMarginMode mode : values()) {
            if (mode.code.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("不支持的保证金模式：" + code);
    }
}
