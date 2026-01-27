package com.iexchange.contract.enums;

import lombok.Getter;

/**
 * 合约订单状态。
 */
@Getter
public enum ContractOrderStatus {

    /**
     * 新建（未成交，可能处于挂单中）。
     */
    NEW("NEW"),
    /**
     * 已成交（全部成交）。
     */
    FILLED("FILLED"),
    /**
     * 已撤销（未成交部分不再继续成交）。
     */
    CANCELED("CANCELED");

    /**
     * 枚举编码。
     */
    private final String code;

    ContractOrderStatus(String code) {
        this.code = code;
    }


}
