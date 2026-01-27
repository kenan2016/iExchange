package com.iexchange.contract.service;

import com.iexchange.contract.entity.ContractSymbolEntity;

/**
 * 合约交易对服务。
 */
public interface ContractSymbolService {

    ContractSymbolEntity getEnabledSymbol(String symbol);

    /**
     * 查询启用的合约交易对列表。
     */
    java.util.List<ContractSymbolEntity> listEnabled();
}
