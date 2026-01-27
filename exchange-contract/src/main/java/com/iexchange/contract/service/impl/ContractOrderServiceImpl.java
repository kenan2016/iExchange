package com.iexchange.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iexchange.contract.client.MarketPriceClient;
import com.iexchange.contract.dto.ContractOrderCancelRequest;
import com.iexchange.contract.dto.ContractOrderRequest;
import com.iexchange.contract.dto.ContractOrderResponse;
import com.iexchange.contract.dto.ContractPositionResponse;
import com.iexchange.contract.entity.ContractFeeFlowEntity;
import com.iexchange.contract.entity.ContractOrderEntity;
import com.iexchange.contract.entity.ContractPositionEntity;
import com.iexchange.contract.entity.ContractSymbolEntity;
import com.iexchange.contract.entity.ContractAccountEntity;
import com.iexchange.contract.enums.ContractMarginMode;
import com.iexchange.contract.enums.ContractOrderAction;
import com.iexchange.contract.enums.ContractOrderStatus;
import com.iexchange.contract.enums.ContractOrderType;
import com.iexchange.contract.enums.ContractPositionSide;
import com.iexchange.contract.mapper.ContractFeeFlowMapper;
import com.iexchange.contract.mapper.ContractOrderMapper;
import com.iexchange.contract.mapper.ContractPositionMapper;
import com.iexchange.contract.service.ContractAccountService;
import com.iexchange.contract.service.ContractOrderService;
import com.iexchange.contract.service.ContractSymbolService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合约订单服务实现。
 */
@Service
public class ContractOrderServiceImpl implements ContractOrderService {

    /**
     * 价格/金额计算保留的小数位（用于保证金、手续费等运算）。
     */
    private static final int PRICE_SCALE = 8;

    private final ContractSymbolService symbolService;
    private final ContractAccountService accountService;
    private final ContractPositionMapper positionMapper;
    private final ContractOrderMapper orderMapper;
    private final ContractFeeFlowMapper feeFlowMapper;
    private final MarketPriceClient marketPriceClient;
    /**
     * 是否允许限价单进入挂单池等待成交。
     */
    private final boolean pendingEnabled;
    /**
     * 最小杠杆倍数（避免 0 或负数导致保证金公式失效）。
     */
    private final int minLeverage;
    /**
     * 手续费费率（按名义价值收取，例：0.0005 = 0.05%）。
     */
    private final BigDecimal feeRate;

    public ContractOrderServiceImpl(ContractSymbolService symbolService,
                                    ContractAccountService accountService,
                                    ContractPositionMapper positionMapper,
                                    ContractOrderMapper orderMapper,
                                    ContractFeeFlowMapper feeFlowMapper,
                                    MarketPriceClient marketPriceClient,
                                    @Value("${contract.order.pending-enabled:true}") boolean pendingEnabled,
                                    @Value("${contract.margin.min-leverage:1}") int minLeverage,
                                    @Value("${contract.fee.rate:0.0005}") BigDecimal feeRate) {
        this.symbolService = symbolService;
        this.accountService = accountService;
        this.positionMapper = positionMapper;
        this.orderMapper = orderMapper;
        this.feeFlowMapper = feeFlowMapper;
        this.marketPriceClient = marketPriceClient;
        this.pendingEnabled = pendingEnabled;
        this.minLeverage = minLeverage;
        this.feeRate = feeRate == null ? BigDecimal.ZERO : feeRate;
    }

    @Override
    @Transactional
    public ContractOrderResponse submitOrder(ContractOrderRequest request) {
        // 1. 校验交易对与参数
        ContractSymbolEntity symbol = symbolService.getEnabledSymbol(request.getSymbol());
        if (symbol == null) {
            throw new IllegalArgumentException("合约交易对不存在或已禁用");
        }
        ContractOrderAction action = ContractOrderAction.fromCode(request.getAction());
        ContractPositionSide side = ContractPositionSide.fromCode(request.getSide());
        ContractOrderType type = ContractOrderType.fromCode(request.getType());
        BigDecimal price = normalizePrice(type, request.getPrice());
        validateScale(symbol, price, request.getQuantity());
        if (action == ContractOrderAction.OPEN) {
            validateOpenRequest(request, symbol);
        } else {
            validateCloseRequest(request);
        }

        // 2. 限价单可进入挂单池等待触发
        if (type == ContractOrderType.LIMIT && pendingEnabled) {
            BigDecimal markPrice = marketPriceClient.getMarkPrice(symbol.getSymbol());
            // 挂单未达到成交条件时，先记录为 NEW 状态
            if (markPrice == null || !shouldExecuteLimitOrder(action, side, price, markPrice)) {
                ContractOrderEntity order = buildOrder(request, symbol.getSymbol(), side, type, price);
                orderMapper.insert(order);
                return ContractOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledPrice(), order.getQuantity());
            }
        }

        // 3. 立即执行开仓或平仓
        if (action == ContractOrderAction.OPEN) {
            return handleOpen(request, symbol, side, type, price);
        }
        return handleClose(request, symbol, side, type, price);
    }

    @Override
    @Transactional
    public ContractOrderResponse cancelOrder(ContractOrderCancelRequest request) {
        ContractOrderEntity order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("无权限撤单");
        }
        if (ContractOrderStatus.FILLED.getCode().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单已成交");
        }
        if (ContractOrderStatus.CANCELED.getCode().equals(order.getStatus())) {
            return ContractOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledPrice(), order.getQuantity());
        }
        // 合约订单撤单仅修改状态（保证金已在挂单时冻结）
        order.setStatus(ContractOrderStatus.CANCELED.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return ContractOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledPrice(), order.getQuantity());
    }

    @Override
    public ContractPositionResponse getPosition(Long userId, String symbol, String side, String marginMode) {
        ContractPositionEntity position = positionMapper.selectOne(new LambdaQueryWrapper<ContractPositionEntity>()
            .eq(ContractPositionEntity::getUserId, userId)
            .eq(ContractPositionEntity::getSymbol, symbol)
            .eq(ContractPositionEntity::getSide, side)
            .eq(ContractPositionEntity::getMarginMode, marginMode));
        if (position == null) {
            throw new IllegalArgumentException("暂无持仓");
        }
        return ContractPositionResponse.ok(position);
    }

    private void validateOpenRequest(ContractOrderRequest request, ContractSymbolEntity symbol) {
        if (request.getMarginMode() == null || request.getMarginMode().isBlank()) {
            throw new IllegalArgumentException("保证金模式不能为空");
        }
        if (request.getLeverage() == null) {
            throw new IllegalArgumentException("杠杆倍数不能为空");
        }
        int leverage = request.getLeverage();
        if (leverage < minLeverage || leverage > symbol.getMaxLeverage()) {
            throw new IllegalArgumentException("杠杆倍数范围：" + minLeverage + "-" + symbol.getMaxLeverage());
        }
    }

    private void validateCloseRequest(ContractOrderRequest request) {
        if (request.getMarginMode() == null || request.getMarginMode().isBlank()) {
            throw new IllegalArgumentException("平仓需要指定保证金模式");
        }
    }

    @Override
    @Transactional
    public boolean matchPendingOrder(ContractOrderEntity order, BigDecimal markPrice) {
        if (order == null || markPrice == null) {
            return false;
        }
        if (!ContractOrderStatus.NEW.getCode().equals(order.getStatus())) {
            return false;
        }
        if (!ContractOrderType.LIMIT.getCode().equals(order.getType())) {
            return false;
        }
        // 按标记价判断挂单是否满足触发条件（避免使用瞬时成交价造成抖动）
        ContractOrderAction action = ContractOrderAction.fromCode(order.getAction());
        ContractPositionSide side = ContractPositionSide.fromCode(order.getSide());
        if (!shouldExecuteLimitOrder(action, side, order.getPrice(), markPrice)) {
            return false;
        }
        ContractSymbolEntity symbol = symbolService.getEnabledSymbol(order.getSymbol());
        if (symbol == null) {
            return false;
        }
        boolean executed;
        if (action == ContractOrderAction.OPEN) {
            executed = executeOpenOrder(order, symbol, side, order.getPrice());
        } else {
            executed = executeCloseOrder(order, symbol, side, order.getPrice());
        }
        return executed;
    }

    private ContractOrderResponse handleOpen(ContractOrderRequest request, ContractSymbolEntity symbol,
                                             ContractPositionSide side, ContractOrderType type, BigDecimal price) {
        ContractOrderEntity order = buildOrder(request, symbol.getSymbol(), side, type, price);
        orderMapper.insert(order);
        if (!executeOpenOrder(order, symbol, side, price)) {
            throw new IllegalArgumentException("开仓失败，保证金不足或参数异常");
        }
        return ContractOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledPrice(), order.getQuantity());
    }

    private ContractOrderResponse handleClose(ContractOrderRequest request, ContractSymbolEntity symbol,
                                              ContractPositionSide side, ContractOrderType type, BigDecimal price) {
        ContractMarginMode.fromCode(request.getMarginMode());
        ContractOrderEntity order = buildOrder(request, symbol.getSymbol(), side, type, price);
        orderMapper.insert(order);
        if (!executeCloseOrder(order, symbol, side, price)) {
            throw new IllegalArgumentException("平仓失败，持仓不足或参数异常");
        }
        return ContractOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledPrice(), order.getQuantity());
    }

    private boolean executeOpenOrder(ContractOrderEntity order, ContractSymbolEntity symbol,
                                     ContractPositionSide side, BigDecimal price) {
        try {
            ContractMarginMode marginMode = ContractMarginMode.fromCode(order.getMarginMode());
            int leverage = order.getLeverage();
            if (leverage < minLeverage || leverage > symbol.getMaxLeverage()) {
                markOrderCanceled(order);
                return false;
            }
            // 计算保证金与手续费：保证金=名义价值/杠杆，手续费=名义价值*费率
            BigDecimal requiredMargin = calculateMargin(price, order.getQuantity(), leverage);
            BigDecimal feeAmount = calculateFee(price, order.getQuantity());
            ContractAccountEntity account;
            try {
                // 扣减保证金与手续费
                account = accountService.deduct(order.getUserId(), requiredMargin.add(feeAmount));
            } catch (IllegalArgumentException ex) {
                markOrderCanceled(order);
                return false;
            }
            BigDecimal accountBalance = account == null ? BigDecimal.ZERO : account.getBalance();

            ContractPositionEntity position = positionMapper.selectOne(new LambdaQueryWrapper<ContractPositionEntity>()
                .eq(ContractPositionEntity::getUserId, order.getUserId())
                .eq(ContractPositionEntity::getSymbol, symbol.getSymbol())
                .eq(ContractPositionEntity::getSide, side.getCode())
                .eq(ContractPositionEntity::getMarginMode, marginMode.getCode()));

            if (position == null) {
                // 首次开仓，创建持仓
                position = new ContractPositionEntity();
                position.setUserId(order.getUserId());
                position.setSymbol(symbol.getSymbol());
                position.setSide(side.getCode());
                position.setMarginMode(marginMode.getCode());
                position.setLeverage(leverage);
                position.setQuantity(order.getQuantity());
                position.setEntryPrice(price);
                position.setMargin(requiredMargin);
                position.setLiquidationPrice(calculateLiquidationPrice(side, price, leverage, marginMode,
                    accountBalance, requiredMargin));
                position.setCreatedAt(LocalDateTime.now());
                position.setUpdatedAt(LocalDateTime.now());
                positionMapper.insert(position);
            } else {
                // 加仓按加权均价更新
                if (!position.getLeverage().equals(leverage)) {
                    markOrderCanceled(order);
                    return false;
                }
                BigDecimal totalQty = position.getQuantity().add(order.getQuantity());
                BigDecimal weightedPrice = position.getEntryPrice().multiply(position.getQuantity())
                    .add(price.multiply(order.getQuantity()))
                    .divide(totalQty, PRICE_SCALE, RoundingMode.HALF_UP);
                position.setQuantity(totalQty);
                position.setEntryPrice(weightedPrice);
                position.setMargin(position.getMargin().add(requiredMargin));
                position.setLiquidationPrice(calculateLiquidationPrice(side, weightedPrice, leverage, marginMode,
                    accountBalance, position.getMargin()));
                position.setUpdatedAt(LocalDateTime.now());
                positionMapper.updateById(position);
            }

            order.setStatus(ContractOrderStatus.FILLED.getCode());
            order.setFilledPrice(price);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            saveFeeFlow(order, feeAmount);
            return true;
        } catch (IllegalArgumentException ex) {
            markOrderCanceled(order);
            return false;
        }
    }

    private boolean executeCloseOrder(ContractOrderEntity order, ContractSymbolEntity symbol,
                                      ContractPositionSide side, BigDecimal price) {
        try {
            ContractMarginMode marginMode = ContractMarginMode.fromCode(order.getMarginMode());
            ContractPositionEntity position = positionMapper.selectOne(new LambdaQueryWrapper<ContractPositionEntity>()
                .eq(ContractPositionEntity::getUserId, order.getUserId())
                .eq(ContractPositionEntity::getSymbol, symbol.getSymbol())
                .eq(ContractPositionEntity::getSide, side.getCode())
                .eq(ContractPositionEntity::getMarginMode, marginMode.getCode()));
            if (position == null || position.getQuantity().compareTo(order.getQuantity()) < 0) {
                markOrderCanceled(order);
                return false;
            }

            // 计算平仓盈亏与释放保证金（盈亏 = 平仓价与开仓价价差 * 数量）
            BigDecimal pnl = calculatePnl(side, position.getEntryPrice(), price, order.getQuantity());
            BigDecimal releaseRatio = order.getQuantity().divide(position.getQuantity(), PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal releaseMargin = position.getMargin().multiply(releaseRatio);
            BigDecimal feeAmount = calculateFee(price, order.getQuantity());
            // 释放保证金 + 盈亏 - 手续费
            accountService.addProfitLoss(order.getUserId(), releaseMargin.add(pnl).subtract(feeAmount));

            BigDecimal remainingQty = position.getQuantity().subtract(order.getQuantity());
            if (remainingQty.compareTo(BigDecimal.ZERO) == 0) {
                positionMapper.deleteById(position.getId());
            } else {
                position.setQuantity(remainingQty);
                position.setMargin(position.getMargin().subtract(releaseMargin));
                ContractAccountEntity account = accountService.getOrCreate(order.getUserId());
                BigDecimal accountBalance = account == null ? BigDecimal.ZERO : account.getBalance();
                // 更新剩余仓位的强平价
                position.setLiquidationPrice(calculateLiquidationPrice(side, position.getEntryPrice(),
                    position.getLeverage(), marginMode, accountBalance, position.getMargin()));
                position.setUpdatedAt(LocalDateTime.now());
                positionMapper.updateById(position);
            }

            order.setStatus(ContractOrderStatus.FILLED.getCode());
            order.setFilledPrice(price);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            saveFeeFlow(order, feeAmount);
            return true;
        } catch (IllegalArgumentException ex) {
            markOrderCanceled(order);
            return false;
        }
    }

    private boolean shouldExecuteLimitOrder(ContractOrderAction action, ContractPositionSide side,
                                            BigDecimal limitPrice, BigDecimal markPrice) {
        if (limitPrice == null || markPrice == null) {
            return false;
        }
        boolean isBuy = isBuyAction(action, side);
        // 买方向：价格越低越容易成交；卖方向：价格越高越容易成交
        if (isBuy) {
            return markPrice.compareTo(limitPrice) <= 0;
        }
        return markPrice.compareTo(limitPrice) >= 0;
    }

    private boolean isBuyAction(ContractOrderAction action, ContractPositionSide side) {
        return (action == ContractOrderAction.OPEN && side == ContractPositionSide.LONG)
            || (action == ContractOrderAction.CLOSE && side == ContractPositionSide.SHORT);
    }

    private void markOrderCanceled(ContractOrderEntity order) {
        order.setStatus(ContractOrderStatus.CANCELED.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private ContractOrderEntity buildOrder(ContractOrderRequest request, String symbol,
                                           ContractPositionSide side, ContractOrderType type, BigDecimal price) {
        ContractOrderEntity order = new ContractOrderEntity();
        order.setUserId(request.getUserId());
        order.setSymbol(symbol);
        order.setAction(request.getAction().trim().toUpperCase());
        order.setSide(side.getCode());
        order.setType(type.getCode());
        order.setPrice(price);
        order.setQuantity(request.getQuantity());
        order.setLeverage(request.getLeverage() == null ? 0 : request.getLeverage());
        order.setMarginMode(request.getMarginMode() == null ? "" : request.getMarginMode().trim().toUpperCase());
        order.setStatus(ContractOrderStatus.NEW.getCode());
        order.setFilledPrice(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    private BigDecimal normalizePrice(ContractOrderType type, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于0");
        }
        // 市价单也需要 price，用作“保护价”或撮合参考价
        return price;
    }

    private BigDecimal calculateMargin(BigDecimal price, BigDecimal quantity, int leverage) {
        // 名义价值 = 价格 * 数量；保证金 = 名义价值 / 杠杆
        return price.multiply(quantity).divide(BigDecimal.valueOf(leverage), PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算手续费。
     */
    private BigDecimal calculateFee(BigDecimal price, BigDecimal quantity) {
        if (feeRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return price.multiply(quantity).multiply(feeRate).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePnl(ContractPositionSide side, BigDecimal entryPrice,
                                    BigDecimal closePrice, BigDecimal quantity) {
        // 多头：平仓价高于开仓价盈利；空头：平仓价低于开仓价盈利
        if (side == ContractPositionSide.LONG) {
            return closePrice.subtract(entryPrice).multiply(quantity);
        }
        return entryPrice.subtract(closePrice).multiply(quantity);
    }

    /**
     * 简化版强平价估算公式。
     *
     * 说明：
     * - 逐仓：主要由开仓价和杠杆决定
     * - 全仓：用账户余额作为缓冲，折算后降低有效杠杆
     */
    private BigDecimal calculateLiquidationPrice(ContractPositionSide side, BigDecimal entryPrice, int leverage,
                                                 ContractMarginMode marginMode,
                                                 BigDecimal accountBalance, BigDecimal positionMargin) {
        BigDecimal effectiveLeverage = BigDecimal.valueOf(leverage);
        if (marginMode == ContractMarginMode.CROSS && positionMargin != null
            && positionMargin.compareTo(BigDecimal.ZERO) > 0 && accountBalance != null) {
            // 全仓：账户余额越多，可抵御的波动越大
            BigDecimal cushion = accountBalance.divide(positionMargin, PRICE_SCALE, RoundingMode.HALF_UP);
            effectiveLeverage = effectiveLeverage.divide(BigDecimal.ONE.add(cushion), PRICE_SCALE, RoundingMode.HALF_UP);
            if (effectiveLeverage.compareTo(BigDecimal.ONE) < 0) {
                effectiveLeverage = BigDecimal.ONE;
            }
        }
        BigDecimal factor = BigDecimal.ONE.divide(effectiveLeverage, PRICE_SCALE, RoundingMode.HALF_UP);
        if (side == ContractPositionSide.LONG) {
            // 多头：价格下跌触发强平
            return entryPrice.multiply(BigDecimal.ONE.subtract(factor));
        }
        // 空头：价格上涨触发强平
        return entryPrice.multiply(BigDecimal.ONE.add(factor));
    }

    private void saveFeeFlow(ContractOrderEntity order, BigDecimal feeAmount) {
        if (feeAmount == null || feeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ContractFeeFlowEntity existing = feeFlowMapper.selectOne(new LambdaQueryWrapper<ContractFeeFlowEntity>()
            .eq(ContractFeeFlowEntity::getOrderId, order.getId()));
        if (existing != null) {
            return;
        }
        ContractFeeFlowEntity flow = new ContractFeeFlowEntity();
        flow.setOrderId(order.getId());
        flow.setUserId(order.getUserId());
        flow.setSymbol(order.getSymbol());
        flow.setAction(order.getAction());
        flow.setSide(order.getSide());
        flow.setFeeRate(feeRate);
        flow.setFeeAmount(feeAmount);
        flow.setCreatedAt(LocalDateTime.now());
        feeFlowMapper.insert(flow);
    }

    /**
     * 校验精度。
     */
    private void validateScale(ContractSymbolEntity symbol, BigDecimal price, BigDecimal quantity) {
        int priceScale = Math.max(price.stripTrailingZeros().scale(), 0);
        if (priceScale > symbol.getPriceScale()) {
            throw new IllegalArgumentException("价格精度超出限制，允许小数位：" + symbol.getPriceScale());
        }
        int quantityScale = Math.max(quantity.stripTrailingZeros().scale(), 0);
        if (quantityScale > symbol.getQuantityScale()) {
            throw new IllegalArgumentException("数量精度超出限制，允许小数位：" + symbol.getQuantityScale());
        }
    }
}
