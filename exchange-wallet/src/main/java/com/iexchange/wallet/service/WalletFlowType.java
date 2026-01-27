package com.iexchange.wallet.service;

import lombok.Getter;

/**
 * 钱包流水类型。
 */
@Getter
public enum WalletFlowType {
    /**
     * 充值入金。
     */
    DEPOSIT("DEPOSIT"),
    /**
     * 提币出金。
     */
    WITHDRAW("WITHDRAW"),
    /**
     * 冻结余额。
     */
    FREEZE("FREEZE"),
    /**
     * 解冻余额。
     */
    UNFREEZE("UNFREEZE"),
    /**
     * 交易扣减。
     */
    DEDUCT("DEDUCT"),
    /**
     * 交易入账。
     */
    TRADE_IN("TRADE_IN");

    /**
     * 枚举编码。
     */
    private final String code;

    WalletFlowType(String code) {
        this.code = code;
    }


}
