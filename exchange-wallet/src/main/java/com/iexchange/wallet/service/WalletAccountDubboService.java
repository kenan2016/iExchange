package com.iexchange.wallet.service;

import com.iexchange.api.wallet.WalletAccountDTO;
import com.iexchange.api.wallet.WalletAccountService;
import com.iexchange.wallet.entity.WalletAccountEntity;
import java.math.BigDecimal;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 钱包 Dubbo 服务实现。
 */
@DubboService
public class WalletAccountDubboService implements WalletAccountService {

    private final WalletService walletService;

    public WalletAccountDubboService(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public WalletAccountDTO freeze(Long userId, String asset, BigDecimal amount, String requestId) {
        WalletAccountEntity account = walletService.freeze(userId, asset, amount, requestId);
        return toDto(account);
    }

    @Override
    public WalletAccountDTO unfreeze(Long userId, String asset, BigDecimal amount, String requestId) {
        WalletAccountEntity account = walletService.unfreeze(userId, asset, amount, requestId);
        return toDto(account);
    }

    @Override
    public WalletAccountDTO deductFrozen(Long userId, String asset, BigDecimal amount, String requestId) {
        WalletAccountEntity account = walletService.deductFrozen(userId, asset, amount, requestId);
        return toDto(account);
    }

    @Override
    public WalletAccountDTO tradeIn(Long userId, String asset, BigDecimal amount, String requestId) {
        WalletAccountEntity account = walletService.tradeIn(userId, asset, amount, requestId);
        return toDto(account);
    }

    @Override
    public WalletAccountDTO getBalance(Long userId, String asset) {
        WalletAccountEntity account = walletService.getBalance(userId, asset);
        return toDto(account);
    }

    private WalletAccountDTO toDto(WalletAccountEntity account) {
        if (account == null) {
            return null;
        }
        WalletAccountDTO dto = new WalletAccountDTO();
        dto.setUserId(account.getUserId());
        dto.setAsset(account.getAsset());
        dto.setAvailableBalance(account.getAvailableBalance());
        dto.setFrozenBalance(account.getFrozenBalance());
        dto.setTotalBalance(account.getTotalBalance());
        return dto;
    }
}
