package com.iexchange.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.wallet.entity.WalletAccountEntity;
import com.iexchange.wallet.entity.WalletFlowEntity;
import com.iexchange.wallet.mapper.WalletAccountMapper;
import com.iexchange.wallet.mapper.WalletFlowMapper;
import com.iexchange.wallet.service.WalletFlowType;
import com.iexchange.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 钱包服务实现。
 */
@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

    private final WalletAccountMapper accountMapper;
    private final WalletFlowMapper flowMapper;
    private final RedissonClient redissonClient;

    public WalletServiceImpl(WalletAccountMapper accountMapper,
                             WalletFlowMapper flowMapper,
                             RedissonClient redissonClient) {
        this.accountMapper = accountMapper;
        this.flowMapper = flowMapper;
        this.redissonClient = redissonClient;
    }

    @Override
    public WalletAccountEntity getBalance(Long userId, String asset) {
        return accountMapper.selectOne(new LambdaQueryWrapper<WalletAccountEntity>()
            .eq(WalletAccountEntity::getUserId, userId)
            .eq(WalletAccountEntity::getAsset, asset));
    }

    @Override
    @Transactional
    public WalletAccountEntity deposit(Long userId, String asset, BigDecimal amount, String requestId) {
        return withLock(userId, asset, () -> {
            WalletAccountEntity account = getOrCreateAccount(userId, asset);
            // 幂等校验：相同业务单号只处理一次
            if (isDuplicateFlow(userId, asset, WalletFlowType.DEPOSIT, requestId)) {
                return getBalance(userId, asset);
            }
            BigDecimal newAvailable = account.getAvailableBalance().add(amount);
            BigDecimal newTotal = account.getTotalBalance().add(amount);
            updateAccount(account, newAvailable, account.getFrozenBalance(), newTotal);
            // 入金落流水
            saveFlow(userId, asset, WalletFlowType.DEPOSIT, amount, newTotal, requestId);
            return account;
        });
    }

    @Override
    @Transactional
    public WalletAccountEntity withdraw(Long userId, String asset, BigDecimal amount, String requestId) {
        return withLock(userId, asset, () -> {
            WalletAccountEntity account = getOrCreateAccount(userId, asset);
            // 幂等校验：相同业务单号只处理一次
            if (isDuplicateFlow(userId, asset, WalletFlowType.WITHDRAW, requestId)) {
                return getBalance(userId, asset);
            }
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("可用余额不足");
            }
            BigDecimal newAvailable = account.getAvailableBalance().subtract(amount);
            BigDecimal newTotal = account.getTotalBalance().subtract(amount);
            updateAccount(account, newAvailable, account.getFrozenBalance(), newTotal);
            // 出金落流水
            saveFlow(userId, asset, WalletFlowType.WITHDRAW, amount, newTotal, requestId);
            return account;
        });
    }

    @Override
    @Transactional
    public WalletAccountEntity freeze(Long userId, String asset, BigDecimal amount, String requestId) {
        return withLock(userId, asset, () -> {
            log.info("freeze start---->");
            WalletAccountEntity account = getOrCreateAccount(userId, asset);
            // 幂等校验：相同业务单号只处理一次
            if (isDuplicateFlow(userId, asset, WalletFlowType.FREEZE, requestId)) {
                return getBalance(userId, asset);
            }
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("可用余额不足");
            }
            BigDecimal newAvailable = account.getAvailableBalance().subtract(amount);
            BigDecimal newFrozen = account.getFrozenBalance().add(amount);
            updateAccount(account, newAvailable, newFrozen, account.getTotalBalance());
            // 冻结落流水
            saveFlow(userId, asset, WalletFlowType.FREEZE, amount, account.getTotalBalance(), requestId);
            return account;
        });
    }

    @Override
    @Transactional
    public WalletAccountEntity unfreeze(Long userId, String asset, BigDecimal amount, String requestId) {
        return withLock(userId, asset, () -> {
            WalletAccountEntity account = getOrCreateAccount(userId, asset);
            // 幂等校验：相同业务单号只处理一次
            if (isDuplicateFlow(userId, asset, WalletFlowType.UNFREEZE, requestId)) {
                return getBalance(userId, asset);
            }
            if (account.getFrozenBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("冻结余额不足");
            }
            BigDecimal newAvailable = account.getAvailableBalance().add(amount);
            BigDecimal newFrozen = account.getFrozenBalance().subtract(amount);
            updateAccount(account, newAvailable, newFrozen, account.getTotalBalance());
            // 解冻落流水
            saveFlow(userId, asset, WalletFlowType.UNFREEZE, amount, account.getTotalBalance(), requestId);
            return account;
        });
    }

    @Override
    @Transactional
    public WalletAccountEntity deductFrozen(Long userId, String asset, BigDecimal amount, String requestId) {
        return withLock(userId, asset, () -> {
            WalletAccountEntity account = getOrCreateAccount(userId, asset);
            // 幂等校验：相同业务单号只处理一次
            if (isDuplicateFlow(userId, asset, WalletFlowType.DEDUCT, requestId)) {
                return getBalance(userId, asset);
            }
            if (account.getFrozenBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("冻结余额不足");
            }
            BigDecimal newFrozen = account.getFrozenBalance().subtract(amount);
            BigDecimal newTotal = account.getTotalBalance().subtract(amount);
            updateAccount(account, account.getAvailableBalance(), newFrozen, newTotal);
            // 扣减冻结落流水
            saveFlow(userId, asset, WalletFlowType.DEDUCT, amount, newTotal, requestId);
            return account;
        });
    }

    @Override
    @Transactional
    public WalletAccountEntity tradeIn(Long userId, String asset, BigDecimal amount, String requestId) {
        return withLock(userId, asset, () -> {
            WalletAccountEntity account = getOrCreateAccount(userId, asset);
            // 幂等校验：相同业务单号只处理一次
            if (isDuplicateFlow(userId, asset, WalletFlowType.TRADE_IN, requestId)) {
                return getBalance(userId, asset);
            }
            BigDecimal newAvailable = account.getAvailableBalance().add(amount);
            BigDecimal newTotal = account.getTotalBalance().add(amount);
            updateAccount(account, newAvailable, account.getFrozenBalance(), newTotal);
            // 成交入账落流水
            saveFlow(userId, asset, WalletFlowType.TRADE_IN, amount, newTotal, requestId);
            return account;
        });
    }

    private WalletAccountEntity getOrCreateAccount(Long userId, String asset) {
        WalletAccountEntity account = getBalance(userId, asset);
        if (account != null) {
            return account;
        }
        // 首次创建钱包账户
        WalletAccountEntity newAccount = new WalletAccountEntity();
        newAccount.setUserId(userId);
        newAccount.setAsset(asset);
        newAccount.setAvailableBalance(BigDecimal.ZERO);
        newAccount.setFrozenBalance(BigDecimal.ZERO);
        newAccount.setTotalBalance(BigDecimal.ZERO);
        newAccount.setCreatedAt(LocalDateTime.now());
        newAccount.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(newAccount);
        return newAccount;
    }

    private boolean isDuplicateFlow(Long userId, String asset, WalletFlowType flowType, String requestId) {
        WalletFlowEntity existing = flowMapper.selectOne(new LambdaQueryWrapper<WalletFlowEntity>()
            .eq(WalletFlowEntity::getUserId, userId)
            .eq(WalletFlowEntity::getAsset, asset)
            .eq(WalletFlowEntity::getFlowType, flowType.getCode())
            .eq(WalletFlowEntity::getBusinessId, requestId));
        return existing != null;
    }

    private <T> T withLock(Long userId, String asset, Supplier<T> action) {
        String lockKey = "wallet:lock:" + userId + ":" + asset;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 并发场景下只允许一个线程修改同一账户
            locked = lock.tryLock(1, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalArgumentException("系统繁忙，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("系统繁忙，请稍后重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void updateAccount(WalletAccountEntity account, BigDecimal available, BigDecimal frozen, BigDecimal total) {
        account.setAvailableBalance(available);
        account.setFrozenBalance(frozen);
        account.setTotalBalance(total);
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    private void saveFlow(Long userId, String asset, WalletFlowType flowType, BigDecimal amount,
                          BigDecimal balanceAfter, String requestId) {
        WalletFlowEntity flow = new WalletFlowEntity();
        flow.setUserId(userId);
        flow.setAsset(asset);
        flow.setFlowType(flowType.getCode());
        flow.setAmount(amount);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessId(requestId);
        flow.setCreatedAt(LocalDateTime.now());
        flowMapper.insert(flow);
    }
}
