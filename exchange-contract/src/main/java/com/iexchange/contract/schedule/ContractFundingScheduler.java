package com.iexchange.contract.schedule;

import com.iexchange.contract.client.MarketPriceClient;
import com.iexchange.contract.dto.FundingRateRequest;
import com.iexchange.contract.dto.FundingRateResponse;
import com.iexchange.contract.dto.FundingSettleRequest;
import com.iexchange.contract.entity.ContractSymbolEntity;
import com.iexchange.contract.service.ContractFundingService;
import com.iexchange.contract.service.ContractSymbolService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 资金费率定时任务。
 */
@Slf4j
@Service
public class ContractFundingScheduler {

    private final ContractSymbolService symbolService;
    private final ContractFundingService fundingService;
    private final MarketPriceClient marketPriceClient;
    /**
     * 是否启用自动资金费率计算。
     */
    private final boolean enabled;
    /**
     * 是否在计算后自动结算资金费。
     */
    private final boolean autoSettle;
    /**
     * 结算触发窗口（秒），用于允许一定时间误差。
     */
    private final long settleWindowSeconds;

    public ContractFundingScheduler(ContractSymbolService symbolService,
                                    ContractFundingService fundingService,
                                    MarketPriceClient marketPriceClient,
                                    @Value("${contract.funding.auto-enabled:false}") boolean enabled,
                                    @Value("${contract.funding.auto-settle:false}") boolean autoSettle,
                                    @Value("${contract.funding.settle-window-seconds:5}") long settleWindowSeconds) {
        this.symbolService = symbolService;
        this.fundingService = fundingService;
        this.marketPriceClient = marketPriceClient;
        this.enabled = enabled;
        this.autoSettle = autoSettle;
        this.settleWindowSeconds = settleWindowSeconds;
    }

    @Scheduled(fixedDelayString = "${contract.funding.auto-interval-ms:60000}")
    public void execute() {
        if (!enabled) {
            return;
        }
        List<ContractSymbolEntity> symbols = symbolService.listEnabled();
        if (symbols.isEmpty()) {
            return;
        }
        long now = nowEpochSeconds();
        for (ContractSymbolEntity symbol : symbols) {
            // 拉取标记价并计算资金费率
            BigDecimal markPrice = marketPriceClient.getMarkPrice(symbol.getSymbol());
            if (markPrice == null) {
                continue;
            }
            FundingRateRequest rateRequest = new FundingRateRequest();
            rateRequest.setSymbol(symbol.getSymbol());
            rateRequest.setMarkPrice(markPrice);
            rateRequest.setIndexPrice(markPrice);
            FundingRateResponse rateResponse;
            try {
                rateResponse = fundingService.calculateRate(rateRequest);
            } catch (IllegalArgumentException ex) {
                log.warn("资金费率计算失败，symbol={}, message={}", symbol.getSymbol(), ex.getMessage());
                continue;
            }
            if (!autoSettle || rateResponse == null) {
                continue;
            }
            // 到达结算窗口才触发结算，避免频繁重复结算
            if (!shouldSettle(now, rateResponse.getNextSettleTime())) {
                continue;
            }
            FundingSettleRequest settleRequest = new FundingSettleRequest();
            settleRequest.setSymbol(symbol.getSymbol());
            settleRequest.setMarkPrice(markPrice);
            settleRequest.setIndexPrice(markPrice);
            try {
                fundingService.settleFunding(settleRequest);
                log.info("触发资金费率结算，symbol={}", symbol.getSymbol());
            } catch (IllegalArgumentException ex) {
                log.warn("资金费率结算失败，symbol={}, message={}", symbol.getSymbol(), ex.getMessage());
            }
        }
    }

    private boolean shouldSettle(long now, long nextSettleTime) {
        if (nextSettleTime <= 0) {
            return false;
        }
        long diff = Math.abs(nextSettleTime - now);
        return diff <= settleWindowSeconds;
    }

    private long nowEpochSeconds() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
