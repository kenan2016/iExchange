package com.iexchange.contract.service;

import com.iexchange.contract.entity.ContractAccountEntity;
import java.math.BigDecimal;

/**
 * 合约保证金账户服务。
 */
public interface ContractAccountService {

    ContractAccountEntity getOrCreate(Long userId);

    ContractAccountEntity deposit(Long userId, BigDecimal amount);

    ContractAccountEntity deduct(Long userId, BigDecimal amount);

    ContractAccountEntity addProfitLoss(Long userId, BigDecimal amount);
}
