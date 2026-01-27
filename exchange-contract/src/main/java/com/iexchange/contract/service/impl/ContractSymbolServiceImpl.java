package com.iexchange.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.entity.ContractSymbolEntity;
import com.iexchange.contract.mapper.ContractSymbolMapper;
import com.iexchange.contract.service.ContractSymbolService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 合约交易对服务实现。
 */
@Service
public class ContractSymbolServiceImpl implements ContractSymbolService {

    private final ContractSymbolMapper symbolMapper;

    public ContractSymbolServiceImpl(ContractSymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
    }

    @Override
    public ContractSymbolEntity getEnabledSymbol(String symbol) {
        return symbolMapper.selectOne(new LambdaQueryWrapper<ContractSymbolEntity>()
            .eq(ContractSymbolEntity::getSymbol, symbol)
            // status=1 表示启用（0 为禁用）
            .eq(ContractSymbolEntity::getStatus, 1));
    }

    @Override
    public List<ContractSymbolEntity> listEnabled() {
        return symbolMapper.selectList(new LambdaQueryWrapper<ContractSymbolEntity>()
            // status=1 表示启用
            .eq(ContractSymbolEntity::getStatus, 1));
    }
}
