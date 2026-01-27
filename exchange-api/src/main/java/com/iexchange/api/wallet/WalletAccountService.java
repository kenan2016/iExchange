package com.iexchange.api.wallet;

import java.math.BigDecimal;

/**
 * 钱包 Dubbo 接口。
 */
public interface WalletAccountService {

    /**
     * 冻结资金。
     */
    WalletAccountDTO freeze(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 解冻资金。
     */
    WalletAccountDTO unfreeze(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 扣减冻结资金（成交扣款）。
     */
    WalletAccountDTO deductFrozen(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 交易入账（增加可用与总额）。
     */
    WalletAccountDTO tradeIn(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 查询余额。
     */
    WalletAccountDTO getBalance(Long userId, String asset);
}
