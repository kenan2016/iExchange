package com.iexchange.spot.client;

import com.iexchange.api.wallet.WalletAccountDTO;
import com.iexchange.api.wallet.WalletAccountService;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 钱包服务客户端。
 */
@Slf4j
@Service
public class WalletAccountClient {

    @DubboReference(check = false, timeout = 6000, retries = 0)
    private WalletAccountService walletAccountService;

    public WalletAccountDTO freeze(Long userId, String asset, BigDecimal amount, String requestId) {
        try {
            return walletAccountService.freeze(userId, asset, amount, requestId);
        } catch (Exception ex) {
            log.warn("钱包冻结失败", ex);
            return null;
        }
    }

    public WalletAccountDTO unfreeze(Long userId, String asset, BigDecimal amount, String requestId) {
        try {
            return walletAccountService.unfreeze(userId, asset, amount, requestId);
        } catch (Exception ex) {
            log.warn("钱包解冻失败", ex);
            return null;
        }
    }

    public WalletAccountDTO deductFrozen(Long userId, String asset, BigDecimal amount, String requestId) {
        try {
            return walletAccountService.deductFrozen(userId, asset, amount, requestId);
        } catch (Exception ex) {
            log.warn("钱包扣减冻结失败", ex);
            return null;
        }
    }

    public WalletAccountDTO tradeIn(Long userId, String asset, BigDecimal amount, String requestId) {
        try {
            return walletAccountService.tradeIn(userId, asset, amount, requestId);
        } catch (Exception ex) {
            log.warn("钱包交易入账失败", ex);
            return null;
        }
    }
}
