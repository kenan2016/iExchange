package com.iexchange.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.entity.ContractAccountEntity;
import com.iexchange.contract.mapper.ContractAccountMapper;
import com.iexchange.contract.service.ContractAccountService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合约保证金账户服务实现。
 */
@Service
public class ContractAccountServiceImpl implements ContractAccountService {

    private final ContractAccountMapper accountMapper;

    public ContractAccountServiceImpl(ContractAccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public ContractAccountEntity getOrCreate(Long userId) {
        ContractAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<ContractAccountEntity>()
            .eq(ContractAccountEntity::getUserId, userId));
        if (account != null) {
            return account;
        }
        ContractAccountEntity newAccount = new ContractAccountEntity();
        newAccount.setUserId(userId);
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setCreatedAt(LocalDateTime.now());
        newAccount.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(newAccount);
        return newAccount;
    }

    @Override
    @Transactional
    public ContractAccountEntity deposit(Long userId, BigDecimal amount) {
        ContractAccountEntity account = getOrCreate(userId);
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
        return account;
    }

    @Override
    @Transactional
    public ContractAccountEntity deduct(Long userId, BigDecimal amount) {
        ContractAccountEntity account = getOrCreate(userId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("保证金余额不足");
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
        return account;
    }

    @Override
    @Transactional
    public ContractAccountEntity addProfitLoss(Long userId, BigDecimal amount) {
        ContractAccountEntity account = getOrCreate(userId);
        account.setBalance(account.getBalance().add(amount));
        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            account.setBalance(BigDecimal.ZERO);
        }
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
        return account;
    }
}
