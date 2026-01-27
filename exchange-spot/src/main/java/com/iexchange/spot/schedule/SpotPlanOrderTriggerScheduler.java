package com.iexchange.spot.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.spot.client.MarketPriceClient;
import com.iexchange.spot.dto.PlaceOrderRequest;
import com.iexchange.spot.dto.SpotOrderResponse;
import com.iexchange.spot.entity.SpotPlanOrderEntity;
import com.iexchange.spot.enums.SpotOrderSide;
import com.iexchange.spot.enums.SpotOrderType;
import com.iexchange.spot.enums.SpotPlanOrderStatus;
import com.iexchange.spot.mapper.SpotPlanOrderMapper;
import com.iexchange.spot.service.SpotOrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 计划委托触发任务。
 */
@Slf4j
@Service
public class SpotPlanOrderTriggerScheduler {
    private final SpotPlanOrderMapper planOrderMapper;
    private final SpotOrderService orderService;
    private final MarketPriceClient marketPriceClient;
    private final boolean enabled;

    public SpotPlanOrderTriggerScheduler(SpotPlanOrderMapper planOrderMapper,
                                         SpotOrderService orderService,
                                         MarketPriceClient marketPriceClient,
                                         @Value("${spot.plan.enabled:true}") boolean enabled) {
        this.planOrderMapper = planOrderMapper;
        this.orderService = orderService;
        this.marketPriceClient = marketPriceClient;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${spot.plan.check-interval-ms:5000}")
    public void checkPlanOrders() {
        if (!enabled) {
            return;
        }
        List<SpotPlanOrderEntity> pendingOrders = planOrderMapper.selectList(
            new LambdaQueryWrapper<SpotPlanOrderEntity>()
                .eq(SpotPlanOrderEntity::getStatus, SpotPlanOrderStatus.NEW.getCode()));
        if (pendingOrders.isEmpty()) {
            return;
        }
        // 按交易对分组，减少行情请求次数
        Map<String, List<SpotPlanOrderEntity>> grouped = pendingOrders.stream()
            .collect(Collectors.groupingBy(SpotPlanOrderEntity::getSymbol));
        for (Map.Entry<String, List<SpotPlanOrderEntity>> entry : grouped.entrySet()) {
            BigDecimal lastPrice = marketPriceClient.getLastPrice(entry.getKey());
            if (lastPrice == null) {
                continue;
            }
            for (SpotPlanOrderEntity planOrder : entry.getValue()) {
                if (shouldTrigger(planOrder, lastPrice)) {
                    triggerPlanOrder(planOrder);
                }
            }
        }
    }

    private boolean shouldTrigger(SpotPlanOrderEntity planOrder, BigDecimal lastPrice) {
        if (SpotOrderSide.BUY.getCode().equals(planOrder.getSide())) {
            return lastPrice.compareTo(planOrder.getTriggerPrice()) >= 0;
        }
        return lastPrice.compareTo(planOrder.getTriggerPrice()) <= 0;
    }

    public void triggerPlanOrder(SpotPlanOrderEntity planOrder) {
        if (!SpotPlanOrderStatus.NEW.getCode().equals(planOrder.getStatus())) {
            return;
        }
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setUserId(planOrder.getUserId());
        request.setSymbol(planOrder.getSymbol());
        request.setSide(planOrder.getSide());
        request.setType(planOrder.getType());
        request.setQuantity(planOrder.getQuantity());
        BigDecimal orderPrice = planOrder.getOrderPrice();
        if (SpotOrderType.MARKET.getCode().equals(planOrder.getType())) {
            // 市价单使用触发价作为保护价
            orderPrice = planOrder.getTriggerPrice();
        }
        request.setPrice(orderPrice);

        try {
            SpotOrderResponse response = orderService.placeOrder(request);
            planOrder.setStatus(SpotPlanOrderStatus.TRIGGERED.getCode());
            planOrder.setTriggeredOrderId(response.getOrderId());
            planOrder.setTriggeredAt(LocalDateTime.now());
            planOrder.setUpdatedAt(LocalDateTime.now());
            planOrderMapper.updateById(planOrder);
            log.info("计划单触发成功，planOrderId={}, orderId={}", planOrder.getId(), response.getOrderId());
        } catch (IllegalArgumentException ex) {
            planOrder.setUpdatedAt(LocalDateTime.now());
            planOrderMapper.updateById(planOrder);
            log.warn("计划单触发失败，planOrderId={}, message={}", planOrder.getId(), ex.getMessage());
        }
    }
}
