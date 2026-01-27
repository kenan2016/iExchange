package com.iexchange.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.dto.ContractPlanOrderCancelRequest;
import com.iexchange.contract.dto.ContractPlanOrderListResponse;
import com.iexchange.contract.dto.ContractPlanOrderRequest;
import com.iexchange.contract.dto.ContractPlanOrderResponse;
import com.iexchange.contract.entity.ContractPlanOrderEntity;
import com.iexchange.contract.entity.ContractSymbolEntity;
import com.iexchange.contract.enums.ContractMarginMode;
import com.iexchange.contract.enums.ContractOrderAction;
import com.iexchange.contract.enums.ContractOrderType;
import com.iexchange.contract.enums.ContractPlanOrderStatus;
import com.iexchange.contract.enums.ContractPositionSide;
import com.iexchange.contract.mapper.ContractPlanOrderMapper;
import com.iexchange.contract.service.ContractPlanOrderService;
import com.iexchange.contract.service.ContractSymbolService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合约计划委托服务实现。
 */
@Service
public class ContractPlanOrderServiceImpl implements ContractPlanOrderService {

    private final ContractPlanOrderMapper planOrderMapper;
    private final ContractSymbolService symbolService;
    /**
     * 最小杠杆倍数（用于开仓计划单校验）。
     */
    private final int minLeverage;

    public ContractPlanOrderServiceImpl(ContractPlanOrderMapper planOrderMapper,
                                        ContractSymbolService symbolService,
                                        @Value("${contract.margin.min-leverage:1}") int minLeverage) {
        this.planOrderMapper = planOrderMapper;
        this.symbolService = symbolService;
        this.minLeverage = minLeverage;
    }

    @Override
    @Transactional
    public ContractPlanOrderResponse placePlanOrder(ContractPlanOrderRequest request) {
        // 1. 校验交易对与参数
        ContractSymbolEntity symbol = symbolService.getEnabledSymbol(request.getSymbol());
        if (symbol == null) {
            throw new IllegalArgumentException("合约交易对不存在或已禁用");
        }
        ContractOrderAction action = ContractOrderAction.fromCode(request.getAction());
        ContractPositionSide side = ContractPositionSide.fromCode(request.getSide());
        ContractOrderType type = ContractOrderType.fromCode(request.getType());
        BigDecimal orderPrice = normalizeOrderPrice(type, request.getOrderPrice());
        validateScale(symbol, request.getTriggerPrice(), orderPrice, request.getQuantity());
        if (action == ContractOrderAction.OPEN) {
            validateOpenRequest(request, symbol);
        } else {
            validateCloseRequest(request);
        }

        // 2. 保存计划单，等待触发任务处理（触发价命中后生成真实订单）
        ContractPlanOrderEntity planOrder = new ContractPlanOrderEntity();
        planOrder.setUserId(request.getUserId());
        planOrder.setSymbol(symbol.getSymbol());
        planOrder.setAction(action.getCode());
        planOrder.setSide(side.getCode());
        planOrder.setType(type.getCode());
        planOrder.setTriggerPrice(request.getTriggerPrice());
        planOrder.setOrderPrice(orderPrice == null ? BigDecimal.ZERO : orderPrice);
        planOrder.setQuantity(request.getQuantity());
        planOrder.setLeverage(request.getLeverage());
        planOrder.setMarginMode(normalizeMarginMode(request.getMarginMode()));
        planOrder.setStatus(ContractPlanOrderStatus.NEW.getCode());
        planOrder.setCreatedAt(LocalDateTime.now());
        planOrder.setUpdatedAt(LocalDateTime.now());
        planOrderMapper.insert(planOrder);
        return ContractPlanOrderResponse.ok(planOrder.getId(), planOrder.getStatus(), planOrder.getTriggeredOrderId());
    }

    @Override
    @Transactional
    public ContractPlanOrderResponse cancelPlanOrder(ContractPlanOrderCancelRequest request) {
        ContractPlanOrderEntity planOrder = planOrderMapper.selectById(request.getPlanOrderId());
        if (planOrder == null) {
            throw new IllegalArgumentException("计划单不存在");
        }
        if (!planOrder.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("无权限撤单");
        }
        if (ContractPlanOrderStatus.TRIGGERED.getCode().equals(planOrder.getStatus())) {
            throw new IllegalArgumentException("计划单已触发，无法撤销");
        }
        if (ContractPlanOrderStatus.CANCELED.getCode().equals(planOrder.getStatus())) {
            return ContractPlanOrderResponse.ok(planOrder.getId(), planOrder.getStatus(), planOrder.getTriggeredOrderId());
        }
        planOrder.setStatus(ContractPlanOrderStatus.CANCELED.getCode());
        planOrder.setUpdatedAt(LocalDateTime.now());
        planOrderMapper.updateById(planOrder);
        return ContractPlanOrderResponse.ok(planOrder.getId(), planOrder.getStatus(), planOrder.getTriggeredOrderId());
    }

    @Override
    public ContractPlanOrderListResponse listOrders(Long userId) {
        // 按用户查询最新计划单
        List<ContractPlanOrderEntity> planOrders = planOrderMapper.selectList(
            new LambdaQueryWrapper<ContractPlanOrderEntity>()
                .eq(ContractPlanOrderEntity::getUserId, userId)
                .orderByDesc(ContractPlanOrderEntity::getId));
        List<ContractPlanOrderListResponse.PlanOrderItem> items = new ArrayList<>();
        for (ContractPlanOrderEntity planOrder : planOrders) {
            ContractPlanOrderListResponse.PlanOrderItem item = new ContractPlanOrderListResponse.PlanOrderItem();
            item.setPlanOrderId(planOrder.getId());
            item.setSymbol(planOrder.getSymbol());
            item.setAction(planOrder.getAction());
            item.setSide(planOrder.getSide());
            item.setType(planOrder.getType());
            item.setTriggerPrice(planOrder.getTriggerPrice());
            item.setOrderPrice(planOrder.getOrderPrice());
            item.setQuantity(planOrder.getQuantity());
            item.setLeverage(planOrder.getLeverage());
            item.setMarginMode(planOrder.getMarginMode());
            item.setStatus(planOrder.getStatus());
            item.setTriggeredOrderId(planOrder.getTriggeredOrderId());
            item.setCreatedAt(planOrder.getCreatedAt());
            item.setTriggeredAt(planOrder.getTriggeredAt());
            items.add(item);
        }
        return ContractPlanOrderListResponse.ok(items);
    }

    private BigDecimal normalizeOrderPrice(ContractOrderType type, BigDecimal orderPrice) {
        if (ContractOrderType.LIMIT == type) {
            if (orderPrice == null || orderPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("限价计划单的委托价必须大于0");
            }
            return orderPrice;
        }
        // 市价计划单不需要委托价，使用 0 作为占位
        return orderPrice == null ? BigDecimal.ZERO : orderPrice;
    }

    private void validateOpenRequest(ContractPlanOrderRequest request, ContractSymbolEntity symbol) {
        if (request.getMarginMode() == null || request.getMarginMode().isBlank()) {
            throw new IllegalArgumentException("保证金模式不能为空");
        }
        if (request.getLeverage() == null) {
            throw new IllegalArgumentException("杠杆倍数不能为空");
        }
        ContractMarginMode.fromCode(request.getMarginMode());
        int leverage = request.getLeverage();
        if (leverage < minLeverage || leverage > symbol.getMaxLeverage()) {
            throw new IllegalArgumentException("杠杆倍数范围：" + minLeverage + "-" + symbol.getMaxLeverage());
        }
    }

    private void validateCloseRequest(ContractPlanOrderRequest request) {
        if (request.getMarginMode() == null || request.getMarginMode().isBlank()) {
            throw new IllegalArgumentException("平仓需要指定保证金模式");
        }
        ContractMarginMode.fromCode(request.getMarginMode());
    }

    private String normalizeMarginMode(String marginMode) {
        if (marginMode == null) {
            return null;
        }
        String trimmed = marginMode.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    /**
     * 校验精度。
     */
    private void validateScale(ContractSymbolEntity symbol,
                               BigDecimal triggerPrice,
                               BigDecimal orderPrice,
                               BigDecimal quantity) {
        // 触发价用于判断是否触发计划单
        int triggerScale = Math.max(triggerPrice.stripTrailingZeros().scale(), 0);
        if (triggerScale > symbol.getPriceScale()) {
            throw new IllegalArgumentException("触发价精度超出限制，允许小数位：" + symbol.getPriceScale());
        }
        // 委托价是触发后下单的价格（市价单可为空）
        if (orderPrice != null) {
            int orderScale = Math.max(orderPrice.stripTrailingZeros().scale(), 0);
            if (orderScale > symbol.getPriceScale()) {
                throw new IllegalArgumentException("委托价精度超出限制，允许小数位：" + symbol.getPriceScale());
            }
        }
        int quantityScale = Math.max(quantity.stripTrailingZeros().scale(), 0);
        if (quantityScale > symbol.getQuantityScale()) {
            throw new IllegalArgumentException("数量精度超出限制，允许小数位：" + symbol.getQuantityScale());
        }
    }
}
