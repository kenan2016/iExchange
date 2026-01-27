package com.iexchange.contract.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.client.MarketPriceClient;
import com.iexchange.contract.dto.ContractOrderRequest;
import com.iexchange.contract.dto.ContractOrderResponse;
import com.iexchange.contract.entity.ContractPositionEntity;
import com.iexchange.contract.enums.ContractOrderAction;
import com.iexchange.contract.enums.ContractOrderType;
import com.iexchange.contract.enums.ContractPositionSide;
import com.iexchange.contract.mapper.ContractPositionMapper;
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
 * 合约强平触发任务。
 */
@Slf4j
@Service
public class ContractLiquidationScheduler {

    private final ContractPositionMapper positionMapper;
    private final ContractOrderService orderService;
    private final MarketPriceClient marketPriceClient;
    /**
     * 是否启用强平检查。
     */
    private final boolean enabled;

    public ContractLiquidationScheduler(ContractPositionMapper positionMapper,
                                        ContractOrderService orderService,
                                        MarketPriceClient marketPriceClient,
                                        @Value("${contract.liquidation.enabled:true}") boolean enabled) {
        this.positionMapper = positionMapper;
        this.orderService = orderService;
        this.marketPriceClient = marketPriceClient;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${contract.liquidation.check-interval-ms:5000}")
    public void checkLiquidations() {
        if (!enabled) {
            return;
        }
        // 扫描全部持仓判断是否触发强平
        List<ContractPositionEntity> positions = positionMapper.selectList(new LambdaQueryWrapper<>());
        if (positions.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> priceCache = new HashMap<>();
        for (ContractPositionEntity position : positions) {
            // 同一交易对复用标记价格，减少请求
            BigDecimal markPrice = priceCache.computeIfAbsent(position.getSymbol(), marketPriceClient::getMarkPrice);
            if (markPrice == null || markPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (shouldLiquidate(position, markPrice)) {
                forceClosePosition(position, markPrice);
            }
        }
    }

    private boolean shouldLiquidate(ContractPositionEntity position, BigDecimal markPrice) {
        if (position.getLiquidationPrice() == null) {
            return false;
        }
        try {
            ContractPositionSide side = ContractPositionSide.fromCode(position.getSide());
            if (side == ContractPositionSide.LONG) {
                // 多头：标记价跌破强平价触发强平
                return markPrice.compareTo(position.getLiquidationPrice()) <= 0;
            }
            // 空头：标记价突破强平价触发强平
            return markPrice.compareTo(position.getLiquidationPrice()) >= 0;
        } catch (IllegalArgumentException ex) {
            log.warn("强平检查失败，positionId={}, message={}", position.getId(), ex.getMessage());
            return false;
        }
    }

    private void forceClosePosition(ContractPositionEntity position, BigDecimal markPrice) {
        if (position.getQuantity() == null || position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // 强平：生成市价平仓订单（以当前标记价作为保护价）
        ContractOrderRequest request = new ContractOrderRequest();
        request.setUserId(position.getUserId());
        request.setSymbol(position.getSymbol());
        request.setAction(ContractOrderAction.CLOSE.getCode());
        request.setSide(position.getSide());
        request.setType(ContractOrderType.MARKET.getCode());
        request.setQuantity(position.getQuantity());
        request.setPrice(markPrice);
        request.setMarginMode(position.getMarginMode());
        request.setLeverage(position.getLeverage());

        try {
            ContractOrderResponse response = orderService.submitOrder(request);
            log.warn("触发强平，positionId={}, orderId={}", position.getId(), response.getOrderId());
        } catch (IllegalArgumentException ex) {
            log.warn("强平失败，positionId={}, message={}", position.getId(), ex.getMessage());
        }
    }
}
