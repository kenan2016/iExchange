package com.iexchange.spot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.spot.entity.SpotSymbolEntity;
import com.iexchange.spot.mapper.SpotSymbolMapper;
import com.iexchange.spot.service.SpotSymbolService;
import org.springframework.stereotype.Service;

/**
 * 交易对服务实现。
 */
@Service
public class SpotSymbolServiceImpl implements SpotSymbolService {

    private final SpotSymbolMapper symbolMapper;

    public SpotSymbolServiceImpl(SpotSymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
    }

    @Override
    public SpotSymbolEntity getEnabledSymbol(String symbol) {
        return symbolMapper.selectOne(new LambdaQueryWrapper<SpotSymbolEntity>()
            .eq(SpotSymbolEntity::getSymbol, symbol)
            .eq(SpotSymbolEntity::getStatus, 1));
    }
}
