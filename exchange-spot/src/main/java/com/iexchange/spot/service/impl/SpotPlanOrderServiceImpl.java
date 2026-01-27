package com.iexchange.spot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.spot.dto.PlanOrderCancelRequest;
import com.iexchange.spot.dto.PlanOrderListResponse;
import com.iexchange.spot.dto.PlanOrderRequest;
import com.iexchange.spot.dto.PlanOrderResponse;
import com.iexchange.spot.entity.SpotPlanOrderEntity;
import com.iexchange.spot.entity.SpotSymbolEntity;
import com.iexchange.spot.enums.SpotOrderSide;
import com.iexchange.spot.enums.SpotOrderType;
import com.iexchange.spot.enums.SpotPlanOrderStatus;
import com.iexchange.spot.mapper.SpotPlanOrderMapper;
import com.iexchange.spot.service.SpotPlanOrderService;
import com.iexchange.spot.service.SpotSymbolService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 计划委托服务实现。
 */
@Service
public class SpotPlanOrderServiceImpl implements SpotPlanOrderService {

    private final SpotPlanOrderMapper planOrderMapper;
    private final SpotSymbolService symbolService;

    public SpotPlanOrderServiceImpl(SpotPlanOrderMapper planOrderMapper,
                                    SpotSymbolService symbolService) {
        this.planOrderMapper = planOrderMapper;
        this.symbolService = symbolService;
    }

    @Override
    @Transactional
    public PlanOrderResponse placePlanOrder(PlanOrderRequest request) {
        SpotSymbolEntity symbol = symbolService.getEnabledSymbol(request.getSymbol());
        if (symbol == null) {
            throw new IllegalArgumentException("交易对不存在或已禁用");
        }
        SpotOrderSide side = SpotOrderSide.fromCode(request.getSide());
        SpotOrderType type = SpotOrderType.fromCode(request.getType());
        BigDecimal orderPrice = normalizeOrderPrice(type, request.getOrderPrice());
        validateScale(symbol, request.getTriggerPrice(), orderPrice, request.getQuantity());

        SpotPlanOrderEntity planOrder = new SpotPlanOrderEntity();
        planOrder.setUserId(request.getUserId());
        planOrder.setSymbol(symbol.getSymbol());
        planOrder.setSide(side.getCode());
        planOrder.setType(type.getCode());
        planOrder.setTriggerPrice(request.getTriggerPrice());
        planOrder.setOrderPrice(orderPrice);
        planOrder.setQuantity(request.getQuantity());
        planOrder.setStatus(SpotPlanOrderStatus.NEW.getCode());
        planOrder.setCreatedAt(LocalDateTime.now());
        planOrder.setUpdatedAt(LocalDateTime.now());
        planOrderMapper.insert(planOrder);
        return PlanOrderResponse.ok(planOrder.getId(), planOrder.getStatus(), planOrder.getTriggeredOrderId());
    }

    @Override
    @Transactional
    public PlanOrderResponse cancelPlanOrder(PlanOrderCancelRequest request) {
        SpotPlanOrderEntity planOrder = planOrderMapper.selectById(request.getPlanOrderId());
        if (planOrder == null) {
            throw new IllegalArgumentException("计划单不存在");
        }
        if (!planOrder.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("无权限撤单");
        }
        if (SpotPlanOrderStatus.TRIGGERED.getCode().equals(planOrder.getStatus())) {
            throw new IllegalArgumentException("计划单已触发，无法撤销");
        }
        if (SpotPlanOrderStatus.CANCELED.getCode().equals(planOrder.getStatus())) {
            return PlanOrderResponse.ok(planOrder.getId(), planOrder.getStatus(), planOrder.getTriggeredOrderId());
        }
        planOrder.setStatus(SpotPlanOrderStatus.CANCELED.getCode());
        planOrder.setUpdatedAt(LocalDateTime.now());
        planOrderMapper.updateById(planOrder);
        return PlanOrderResponse.ok(planOrder.getId(), planOrder.getStatus(), planOrder.getTriggeredOrderId());
    }

    @Override
    public PlanOrderListResponse listOrders(Long userId) {
        List<SpotPlanOrderEntity> planOrders = planOrderMapper.selectList(
            new LambdaQueryWrapper<SpotPlanOrderEntity>()
                .eq(SpotPlanOrderEntity::getUserId, userId)
                .orderByDesc(SpotPlanOrderEntity::getId));
        List<PlanOrderListResponse.PlanOrderItem> items = new ArrayList<>();
        for (SpotPlanOrderEntity planOrder : planOrders) {
            PlanOrderListResponse.PlanOrderItem item = new PlanOrderListResponse.PlanOrderItem();
            item.setPlanOrderId(planOrder.getId());
            item.setSymbol(planOrder.getSymbol());
            item.setSide(planOrder.getSide());
            item.setType(planOrder.getType());
            item.setTriggerPrice(planOrder.getTriggerPrice());
            item.setOrderPrice(planOrder.getOrderPrice());
            item.setQuantity(planOrder.getQuantity());
            item.setStatus(planOrder.getStatus());
            item.setTriggeredOrderId(planOrder.getTriggeredOrderId());
            item.setCreatedAt(planOrder.getCreatedAt());
            item.setTriggeredAt(planOrder.getTriggeredAt());
            items.add(item);
        }
        return PlanOrderListResponse.ok(items);
    }

    private BigDecimal normalizeOrderPrice(SpotOrderType type, BigDecimal orderPrice) {
        if (SpotOrderType.LIMIT == type) {
            if (orderPrice == null || orderPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("限价计划单的委托价必须大于0");
            }
            return orderPrice;
        }
        return orderPrice == null ? BigDecimal.ZERO : orderPrice;
    }

    /**
     * 校验精度。
     */
    private void validateScale(SpotSymbolEntity symbol,
                               BigDecimal triggerPrice,
                               BigDecimal orderPrice,
                               BigDecimal quantity) {
        int triggerScale = Math.max(triggerPrice.stripTrailingZeros().scale(), 0);
        if (triggerScale > symbol.getPriceScale()) {
            throw new IllegalArgumentException("触发价精度超出限制，允许小数位：" + symbol.getPriceScale());
        }
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
