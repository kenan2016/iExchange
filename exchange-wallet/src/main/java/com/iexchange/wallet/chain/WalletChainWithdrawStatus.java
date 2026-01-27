package com.iexchange.wallet.chain;

import lombok.Getter;

/**
 * 链上提币状态。
 */
@Getter
public enum WalletChainWithdrawStatus {

    /**
     * 已创建待提交。
     */
    PENDING("PENDING"),
    /**
     * 已提交链上。
     */
    SUBMITTED("SUBMITTED"),
    /**
     * 已确认成功。
     */
    CONFIRMED("CONFIRMED"),
    /**
     * 失败或回滚。
     */
    FAILED("FAILED");

    private final String code;

    WalletChainWithdrawStatus(String code) {
        this.code = code;
    }
}
