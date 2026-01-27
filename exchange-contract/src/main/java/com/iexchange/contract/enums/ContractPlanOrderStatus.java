package com.iexchange.contract.enums;

import lombok.Getter;

/**
 * 合约计划委托状态。
 */
@Getter
public enum ContractPlanOrderStatus {
    /**
     * 新建（等待触发条件满足）。
     */
    NEW("NEW"),
    /**
     * 已触发（触发成功并创建了真实订单）。
     */
    TRIGGERED("TRIGGERED"),
    /**
     * 已撤销（不再触发）。
     */
    CANCELED("CANCELED");

    /**
     * 枚举编码。
     */
    private final String code;

    ContractPlanOrderStatus(String code) {
        this.code = code;
    }
}
