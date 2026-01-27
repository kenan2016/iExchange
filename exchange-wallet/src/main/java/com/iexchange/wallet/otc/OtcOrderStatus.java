package com.iexchange.wallet.otc;

/**
 * OTC 订单状态。
 */
public enum OtcOrderStatus {
    WAIT_PAY("WAIT_PAY"),
    WAIT_RELEASE("WAIT_RELEASE"),
    APPEAL("APPEAL"),
    DONE("DONE"),
    CANCELED("CANCELED");

    private final String code;

    OtcOrderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
