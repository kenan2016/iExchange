package com.iexchange.wallet.service;

import com.iexchange.wallet.entity.WalletAccountEntity;
import java.math.BigDecimal;

/**
 * 钱包服务接口。
 */
public interface WalletService {

    /**
     * 查询余额。
     *
     * @param userId 用户 ID
     * @param asset  资产
     * @return 钱包账户
     */
    WalletAccountEntity getBalance(Long userId, String asset);

    /**
     * 入金。
     */
    WalletAccountEntity deposit(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 出金。
     */
    WalletAccountEntity withdraw(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 冻结资金。
     */
    WalletAccountEntity freeze(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 解冻资金。
     */
    WalletAccountEntity unfreeze(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 扣减冻结（成交扣款）。
     */
    WalletAccountEntity deductFrozen(Long userId, String asset, BigDecimal amount, String requestId);

    /**
     * 交易入账（增加可用与总额）。
     */
    WalletAccountEntity tradeIn(Long userId, String asset, BigDecimal amount, String requestId);
}
