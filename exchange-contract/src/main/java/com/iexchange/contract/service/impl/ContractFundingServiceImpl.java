package com.iexchange.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.dto.FundingRateRequest;
import com.iexchange.contract.dto.FundingRateResponse;
import com.iexchange.contract.dto.FundingSettleRequest;
import com.iexchange.contract.dto.FundingSettleResponse;
import com.iexchange.contract.entity.ContractFundingRateEntity;
import com.iexchange.contract.entity.ContractFundingSettlementEntity;
import com.iexchange.contract.entity.ContractPositionEntity;
import com.iexchange.contract.entity.ContractSymbolEntity;
import com.iexchange.contract.enums.ContractPositionSide;
import com.iexchange.contract.mapper.ContractFundingRateMapper;
import com.iexchange.contract.mapper.ContractFundingSettlementMapper;
import com.iexchange.contract.mapper.ContractPositionMapper;
import com.iexchange.contract.service.ContractAccountService;
import com.iexchange.contract.service.ContractFundingService;
import com.iexchange.contract.service.ContractSymbolService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资金费率服务实现。
 */
@Service
public class ContractFundingServiceImpl implements ContractFundingService {

    /**
     * 资金费率与结算金额的保留小数位。
     */
    private static final int RATE_SCALE = 8;

    private final ContractSymbolService symbolService;
    private final ContractAccountService accountService;
    private final ContractPositionMapper positionMapper;
    private final ContractFundingRateMapper rateMapper;
    private final ContractFundingSettlementMapper settlementMapper;
    /**
     * 资金费率兜底值（行情缺失时使用）。
     */
    private final BigDecimal defaultRate;
    /**
     * 资金费率上限（防止费率过大）。
     */
    private final BigDecimal maxRate;
    /**
     * 费率系数（用于缩放 mark/index 的价差比例）。
     */
    private final BigDecimal rateCoefficient;
    /**
     * 资金费率结算周期（秒），默认 28800 = 8 小时。
     */
    private final long intervalSeconds;

    public ContractFundingServiceImpl(ContractSymbolService symbolService,
                                      ContractAccountService accountService,
                                      ContractPositionMapper positionMapper,
                                      ContractFundingRateMapper rateMapper,
                                      ContractFundingSettlementMapper settlementMapper,
                                      @Value("${contract.funding.default-rate:0.0001}") BigDecimal defaultRate,
                                      @Value("${contract.funding.max-rate:0.003}") BigDecimal maxRate,
                                      @Value("${contract.funding.rate-coefficient:0.01}") BigDecimal rateCoefficient,
                                      @Value("${contract.funding.interval-seconds:28800}") long intervalSeconds) {
        this.symbolService = symbolService;
        this.accountService = accountService;
        this.positionMapper = positionMapper;
        this.rateMapper = rateMapper;
        this.settlementMapper = settlementMapper;
        this.defaultRate = defaultRate;
        this.maxRate = maxRate;
        this.rateCoefficient = rateCoefficient;
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public FundingRateResponse calculateRate(FundingRateRequest request) {
        ContractSymbolEntity symbol = symbolService.getEnabledSymbol(request.getSymbol());
        if (symbol == null) {
            throw new IllegalArgumentException("合约交易对不存在或已禁用");
        }
        // 计算资金费率并保存快照
        BigDecimal rate = calculateFundingRate(request.getMarkPrice(), request.getIndexPrice());
        long now = nowEpochSeconds();
        // 向上对齐到下一次结算点
        long nextSettleTime = now - now % intervalSeconds + intervalSeconds;
        ContractFundingRateEntity entity = new ContractFundingRateEntity();
        entity.setSymbol(symbol.getSymbol());
        entity.setRate(rate);
        entity.setMarkPrice(request.getMarkPrice());
        entity.setIndexPrice(request.getIndexPrice());
        entity.setNextSettleTime(nextSettleTime);
        entity.setCreatedAt(LocalDateTime.now());
        rateMapper.insert(entity);
        return FundingRateResponse.ok(symbol.getSymbol(), rate, nextSettleTime);
    }

    @Override
    public FundingRateResponse getLatestRate(String symbol) {
        ContractFundingRateEntity latest = rateMapper.selectOne(new LambdaQueryWrapper<ContractFundingRateEntity>()
            .eq(ContractFundingRateEntity::getSymbol, symbol)
            .orderByDesc(ContractFundingRateEntity::getId)
            .last("limit 1"));
        if (latest == null) {
            throw new IllegalArgumentException("暂无资金费率");
        }
        return FundingRateResponse.ok(symbol, latest.getRate(), latest.getNextSettleTime());
    }

    @Override
    @Transactional
    public FundingSettleResponse settleFunding(FundingSettleRequest request) {
        ContractSymbolEntity symbol = symbolService.getEnabledSymbol(request.getSymbol());
        if (symbol == null) {
            throw new IllegalArgumentException("合约交易对不存在或已禁用");
        }
        // 结算时重新计算费率，按持仓计算资金费
        BigDecimal rate = calculateFundingRate(request.getMarkPrice(), request.getIndexPrice());
        List<ContractPositionEntity> positions = positionMapper.selectList(
            new LambdaQueryWrapper<ContractPositionEntity>()
                .eq(ContractPositionEntity::getSymbol, symbol.getSymbol()));
        BigDecimal totalAmount = BigDecimal.ZERO;
        long settleTime = nowEpochSeconds();
        for (ContractPositionEntity position : positions) {
            // 资金费 = 名义价值 * 费率（正费率时多头支付，负费率时多头收取）
            BigDecimal notional = request.getMarkPrice().multiply(position.getQuantity());
            BigDecimal fundingAmount = notional.multiply(rate).setScale(RATE_SCALE, RoundingMode.HALF_UP);
            if (ContractPositionSide.LONG.getCode().equals(position.getSide())) {
                fundingAmount = fundingAmount.negate();
            }
            accountService.addProfitLoss(position.getUserId(), fundingAmount);
            ContractFundingSettlementEntity settlement = new ContractFundingSettlementEntity();
            settlement.setPositionId(position.getId());
            settlement.setUserId(position.getUserId());
            settlement.setSymbol(symbol.getSymbol());
            settlement.setSide(position.getSide());
            settlement.setRate(rate);
            settlement.setMarkPrice(request.getMarkPrice());
            settlement.setFundingAmount(fundingAmount);
            settlement.setSettlementTime(settleTime);
            settlement.setCreatedAt(LocalDateTime.now());
            settlementMapper.insert(settlement);
            totalAmount = totalAmount.add(fundingAmount);
        }
        return FundingSettleResponse.ok(symbol.getSymbol(), rate, positions.size(), totalAmount);
    }

    private BigDecimal calculateFundingRate(BigDecimal markPrice, BigDecimal indexPrice) {
        if (markPrice == null || indexPrice == null) {
            return defaultRate;
        }
        BigDecimal diff = markPrice.subtract(indexPrice);
        if (indexPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return defaultRate;
        }
        // 简化公式：费率 = (标记价 - 指数价) / 指数价 * 系数，并限制上下限
        BigDecimal raw = diff.divide(indexPrice, RATE_SCALE, RoundingMode.HALF_UP)
            .multiply(rateCoefficient)
            .setScale(RATE_SCALE, RoundingMode.HALF_UP);
        if (raw.compareTo(maxRate) > 0) {
            return maxRate;
        }
        if (raw.compareTo(maxRate.negate()) < 0) {
            return maxRate.negate();
        }
        return raw;
    }

    private long nowEpochSeconds() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
