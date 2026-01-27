package com.iexchange.spot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.spot.entity.SpotFeeFlowEntity;
import com.iexchange.spot.entity.SpotOrderEntity;
import com.iexchange.spot.entity.SpotTradeEntity;
import com.iexchange.spot.mapper.SpotFeeFlowMapper;
import com.iexchange.spot.service.SpotFeeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 现货手续费服务实现。
 */
@Service
public class SpotFeeServiceImpl implements SpotFeeService {

    private static final int FEE_SCALE = 8;

    private final SpotFeeFlowMapper feeFlowMapper;
    private final BigDecimal feeRate;

    public SpotFeeServiceImpl(SpotFeeFlowMapper feeFlowMapper,
                              @Value("${spot.fee.rate:0.001}") BigDecimal feeRate) {
        this.feeFlowMapper = feeFlowMapper;
        this.feeRate = feeRate == null ? BigDecimal.ZERO : feeRate;
    }

    @Override
    public void recordTradeFee(SpotTradeEntity trade, SpotOrderEntity buyOrder, SpotOrderEntity sellOrder) {
        if (trade == null || feeRate.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal notional = trade.getPrice().multiply(trade.getQuantity());
        BigDecimal feeAmount = notional.multiply(feeRate).setScale(FEE_SCALE, RoundingMode.HALF_UP);
        if (feeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String feeAsset = resolveQuoteAsset(trade.getSymbol());
        saveFlow(trade, buyOrder, feeAsset, feeAmount);
        saveFlow(trade, sellOrder, feeAsset, feeAmount);
    }

    private void saveFlow(SpotTradeEntity trade, SpotOrderEntity order, String feeAsset, BigDecimal feeAmount) {
        if (order == null) {
            return;
        }
        SpotFeeFlowEntity existing = feeFlowMapper.selectOne(new LambdaQueryWrapper<SpotFeeFlowEntity>()
            .eq(SpotFeeFlowEntity::getTradeId, trade.getId())
            .eq(SpotFeeFlowEntity::getUserId, order.getUserId())
            .eq(SpotFeeFlowEntity::getSide, order.getSide()));
        if (existing != null) {
            return;
        }
        SpotFeeFlowEntity flow = new SpotFeeFlowEntity();
        flow.setTradeId(trade.getId());
        flow.setUserId(order.getUserId());
        flow.setSymbol(trade.getSymbol());
        flow.setSide(order.getSide());
        flow.setFeeAsset(feeAsset);
        flow.setFeeRate(feeRate);
        flow.setFeeAmount(feeAmount);
        flow.setCreatedAt(LocalDateTime.now());
        feeFlowMapper.insert(flow);
    }

    /**
     * 简化解析计价资产。
     */
    private String resolveQuoteAsset(String symbol) {
        if (symbol == null) {
            return "";
        }
        int index = symbol.indexOf('_');
        if (index > 0 && index < symbol.length() - 1) {
            return symbol.substring(index + 1);
        }
        return symbol;
    }
}
