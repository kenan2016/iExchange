package com.iexchange.contract.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.client.MarketPriceClient;
import com.iexchange.contract.dto.ContractOrderRequest;
import com.iexchange.contract.dto.ContractOrderResponse;
import com.iexchange.contract.entity.ContractPlanOrderEntity;
import com.iexchange.contract.enums.ContractOrderAction;
import com.iexchange.contract.enums.ContractOrderType;
import com.iexchange.contract.enums.ContractPlanOrderStatus;
import com.iexchange.contract.enums.ContractPositionSide;
import com.iexchange.contract.mapper.ContractPlanOrderMapper;
import com.iexchange.contract.service.ContractOrderService;
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
 * 合约计划委托触发任务。
 */
@Slf4j
@Service
public class ContractPlanOrderTriggerScheduler {

    private final ContractPlanOrderMapper planOrderMapper;
    private final ContractOrderService orderService;
    private final MarketPriceClient marketPriceClient;
    /**
     * 是否启用计划委托触发。
     */
    private final boolean enabled;

    public ContractPlanOrderTriggerScheduler(ContractPlanOrderMapper planOrderMapper,
                                             ContractOrderService orderService,
                                             MarketPriceClient marketPriceClient,
                                             @Value("${contract.plan.enabled:true}") boolean enabled) {
        this.planOrderMapper = planOrderMapper;
        this.orderService = orderService;
        this.marketPriceClient = marketPriceClient;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${contract.plan.check-interval-ms:5000}")
    public void checkPlanOrders() {
        if (!enabled) {
            return;
        }
        List<ContractPlanOrderEntity> pendingOrders = planOrderMapper.selectList(
            new LambdaQueryWrapper<ContractPlanOrderEntity>()
                .eq(ContractPlanOrderEntity::getStatus, ContractPlanOrderStatus.NEW.getCode()));
        if (pendingOrders.isEmpty()) {
            return;
        }
        // 按交易对分组，减少行情请求次数
        Map<String, List<ContractPlanOrderEntity>> grouped = pendingOrders.stream()
            .collect(Collectors.groupingBy(ContractPlanOrderEntity::getSymbol));
        for (Map.Entry<String, List<ContractPlanOrderEntity>> entry : grouped.entrySet()) {
            BigDecimal markPrice = marketPriceClient.getMarkPrice(entry.getKey());
            if (markPrice == null) {
                continue;
            }
            for (ContractPlanOrderEntity planOrder : entry.getValue()) {
                if (shouldTrigger(planOrder, markPrice)) {
                    triggerPlanOrder(planOrder, markPrice);
                }
            }
        }
    }

    private boolean shouldTrigger(ContractPlanOrderEntity planOrder, BigDecimal markPrice) {
        if (planOrder.getTriggerPrice() == null || markPrice == null) {
            return false;
        }
        try {
            ContractOrderAction action = ContractOrderAction.fromCode(planOrder.getAction());
            ContractPositionSide side = ContractPositionSide.fromCode(planOrder.getSide());
            boolean isBuy = isBuyAction(action, side);
            // 买方向：价格向上触发；卖方向：价格向下触发
            if (isBuy) {
                return markPrice.compareTo(planOrder.getTriggerPrice()) >= 0;
            }
            return markPrice.compareTo(planOrder.getTriggerPrice()) <= 0;
        } catch (IllegalArgumentException ex) {
            log.warn("计划单触发条件异常，planOrderId={}, message={}", planOrder.getId(), ex.getMessage());
            return false;
        }
    }

    private boolean isBuyAction(ContractOrderAction action, ContractPositionSide side) {
        return (action == ContractOrderAction.OPEN && side == ContractPositionSide.LONG)
            || (action == ContractOrderAction.CLOSE && side == ContractPositionSide.SHORT);
    }

    private void triggerPlanOrder(ContractPlanOrderEntity planOrder, BigDecimal markPrice) {
        if (!ContractPlanOrderStatus.NEW.getCode().equals(planOrder.getStatus())) {
            return;
        }
        ContractOrderRequest request = new ContractOrderRequest();
        request.setUserId(planOrder.getUserId());
        request.setSymbol(planOrder.getSymbol());
        request.setAction(planOrder.getAction());
        request.setSide(planOrder.getSide());
        request.setType(planOrder.getType());
        request.setQuantity(planOrder.getQuantity());
        request.setLeverage(planOrder.getLeverage());
        request.setMarginMode(planOrder.getMarginMode());
        BigDecimal orderPrice = planOrder.getOrderPrice();
        if (ContractOrderType.MARKET.getCode().equals(planOrder.getType())) {
            // 市价单使用触发价作为保护价
            orderPrice = planOrder.getTriggerPrice();
        }
        request.setPrice(orderPrice == null || orderPrice.compareTo(BigDecimal.ZERO) <= 0 ? markPrice : orderPrice);

        try {
            ContractOrderResponse response = orderService.submitOrder(request);
            planOrder.setStatus(ContractPlanOrderStatus.TRIGGERED.getCode());
            planOrder.setTriggeredOrderId(response.getOrderId());
            planOrder.setTriggeredAt(LocalDateTime.now());
            planOrder.setUpdatedAt(LocalDateTime.now());
            planOrderMapper.updateById(planOrder);
            log.info("合约计划单触发成功，planOrderId={}, orderId={}", planOrder.getId(), response.getOrderId());
        } catch (IllegalArgumentException ex) {
            planOrder.setUpdatedAt(LocalDateTime.now());
            planOrderMapper.updateById(planOrder);
            log.warn("合约计划单触发失败，planOrderId={}, message={}", planOrder.getId(), ex.getMessage());
        }
    }
}
