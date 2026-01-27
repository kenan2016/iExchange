package com.iexchange.contract.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.client.MarketPriceClient;
import com.iexchange.contract.entity.ContractOrderEntity;
import com.iexchange.contract.enums.ContractOrderStatus;
import com.iexchange.contract.enums.ContractOrderType;
import com.iexchange.contract.mapper.ContractOrderMapper;
import com.iexchange.contract.service.ContractOrderService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 合约挂单撮合任务。
 */
@Slf4j
@Service
public class ContractOrderMatchScheduler {

    private final ContractOrderMapper orderMapper;
    private final ContractOrderService orderService;
    private final MarketPriceClient marketPriceClient;
    /**
     * 是否启用挂单撮合。
     */
    private final boolean enabled;

    public ContractOrderMatchScheduler(ContractOrderMapper orderMapper,
                                       ContractOrderService orderService,
                                       MarketPriceClient marketPriceClient,
                                       @Value("${contract.order.match-enabled:true}") boolean enabled) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.marketPriceClient = marketPriceClient;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${contract.order.match-interval-ms:3000}")
    public void matchPendingOrders() {
        if (!enabled) {
            return;
        }
        List<ContractOrderEntity> pendingOrders = orderMapper.selectList(
            new LambdaQueryWrapper<ContractOrderEntity>()
                .eq(ContractOrderEntity::getStatus, ContractOrderStatus.NEW.getCode())
                .eq(ContractOrderEntity::getType, ContractOrderType.LIMIT.getCode()));
        if (pendingOrders.isEmpty()) {
            return;
        }
        // 按交易对缓存标记价，避免重复请求
        Map<String, BigDecimal> priceCache = new HashMap<>();
        for (ContractOrderEntity order : pendingOrders) {
            BigDecimal markPrice = priceCache.computeIfAbsent(order.getSymbol(), marketPriceClient::getMarkPrice);
            if (markPrice == null) {
                continue;
            }
            boolean matched = orderService.matchPendingOrder(order, markPrice);
            if (matched) {
                log.info("合约挂单成交，orderId={}", order.getId());
            }
        }
    }
}
