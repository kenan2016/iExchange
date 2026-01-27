package com.iexchange.spot.service.impl;

import com.iexchange.spot.client.WalletAccountClient;
import com.iexchange.spot.dto.CancelOrderRequest;
import com.iexchange.spot.dto.PlaceOrderRequest;
import com.iexchange.spot.dto.SpotOrderDetailResponse;
import com.iexchange.spot.dto.SpotOrderResponse;
import com.iexchange.spot.engine.SpotMatchingEngine;
import com.iexchange.spot.entity.SpotOrderEntity;
import com.iexchange.spot.entity.SpotSymbolEntity;
import com.iexchange.spot.enums.SpotOrderSide;
import com.iexchange.spot.enums.SpotOrderStatus;
import com.iexchange.spot.enums.SpotOrderType;
import com.iexchange.spot.mapper.SpotOrderMapper;
import com.iexchange.spot.service.SpotOrderService;
import com.iexchange.spot.service.SpotSymbolService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 现货订单服务实现。
 */
@Service
public class SpotOrderServiceImpl implements SpotOrderService {

    private static final int FEE_SCALE = 8;

    private final SpotOrderMapper orderMapper;
    private final SpotSymbolService symbolService;
    private final SpotMatchingEngine matchingEngine;
    private final WalletAccountClient walletClient;
    private final BigDecimal feeRate;

    public SpotOrderServiceImpl(SpotOrderMapper orderMapper,
                                SpotSymbolService symbolService,
                                SpotMatchingEngine matchingEngine,
                                WalletAccountClient walletClient,
                                @Value("${spot.fee.rate:0.001}") BigDecimal feeRate) {
        this.orderMapper = orderMapper;
        this.symbolService = symbolService;
        this.matchingEngine = matchingEngine;
        this.walletClient = walletClient;
        this.feeRate = feeRate == null ? BigDecimal.ZERO : feeRate;
    }

    @Override
    public SpotOrderResponse placeOrder(PlaceOrderRequest request) {
        // 1. 校验交易对与下单参数
        SpotSymbolEntity symbol = symbolService.getEnabledSymbol(request.getSymbol());
        if (symbol == null) {
            throw new IllegalArgumentException("交易对不存在或已禁用");
        }
        SpotOrderSide side = SpotOrderSide.fromCode(request.getSide());
        SpotOrderType type = SpotOrderType.fromCode(request.getType());
        BigDecimal price = normalizePrice(type, request.getPrice());
        validateScale(symbol, price, request.getQuantity());

        // 2. 落库订单，状态先置为 NEW
        SpotOrderEntity order = new SpotOrderEntity();
        order.setUserId(request.getUserId());
        order.setSymbol(symbol.getSymbol());
        order.setSide(side.getCode());
        order.setType(type.getCode());
        order.setPrice(price);
        order.setQuantity(request.getQuantity());
        order.setFilledQuantity(BigDecimal.ZERO);
        order.setStatus(SpotOrderStatus.NEW.getCode());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        // 3. 冻结资金，失败则撤销订单
        if (!freezeBalance(order, symbol)) {
            order.setStatus(SpotOrderStatus.CANCELED.getCode());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            throw new IllegalArgumentException("资金冻结失败");
        }

        // 4. 进入撮合引擎
        matchingEngine.match(order);

        // 5. 返回成交与剩余数量
        BigDecimal remaining = order.getQuantity().subtract(order.getFilledQuantity());
        return SpotOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledQuantity(), remaining);
    }

    @Override
    public SpotOrderResponse cancelOrder(CancelOrderRequest request) {
        SpotOrderEntity order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("无权限撤单");
        }
        if (SpotOrderStatus.FILLED.getCode().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单已全部成交");
        }
        if (SpotOrderStatus.CANCELED.getCode().equals(order.getStatus())) {
            return SpotOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledQuantity(),
                order.getQuantity().subtract(order.getFilledQuantity()));
        }
        // 市价单不进入订单簿，无需撮合撤单
        if (SpotOrderType.MARKET.getCode().equals(order.getType())) {
            return SpotOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledQuantity(),
                order.getQuantity().subtract(order.getFilledQuantity()));
        }
        // 限价单从订单簿移除并解冻剩余资金
        matchingEngine.cancel(order);
        order.setStatus(SpotOrderStatus.CANCELED.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        unfreezeRemaining(order, symbolService.getEnabledSymbol(order.getSymbol()));
        BigDecimal remaining = order.getQuantity().subtract(order.getFilledQuantity());
        return SpotOrderResponse.ok(order.getId(), order.getStatus(), order.getFilledQuantity(), remaining);
    }

    @Override
    public SpotOrderDetailResponse getOrder(Long orderId) {
        SpotOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return SpotOrderDetailResponse.ok(order);
    }

    private BigDecimal normalizePrice(SpotOrderType type, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            if (SpotOrderType.MARKET == type) {
                throw new IllegalArgumentException("市价单必须提供保护价格");
            }
            throw new IllegalArgumentException("限价单价格必须大于0");
        }
        return price;
    }

    /**
     * 校验精度。
     */
    private void validateScale(SpotSymbolEntity symbol, BigDecimal price, BigDecimal quantity) {
        if (price != null) {
            int actualScale = Math.max(price.stripTrailingZeros().scale(), 0);
            if (actualScale > symbol.getPriceScale()) {
                throw new IllegalArgumentException("价格精度超出限制，允许小数位：" + symbol.getPriceScale());
            }
        }
        int quantityScale = Math.max(quantity.stripTrailingZeros().scale(), 0);
        if (quantityScale > symbol.getQuantityScale()) {
            throw new IllegalArgumentException("数量精度超出限制，允许小数位：" + symbol.getQuantityScale());
        }
    }

    /**
     * 冻结下单所需资金。
     *
     * 买单冻结计价币（报价币），卖单冻结基础币。
     */
    private boolean freezeBalance(SpotOrderEntity order, SpotSymbolEntity symbol) {
        if (symbol == null) {
            return false;
        }
        BigDecimal amount = calculateFreezeAmount(order, symbol);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        // 用订单 ID 生成幂等键，避免重复冻结
        String requestId = "spot-freeze-" + order.getId();
        return walletClient.freeze(order.getUserId(), resolveFreezeAsset(order, symbol), amount, requestId) != null;
    }

    /**
     * 撤单后解冻剩余未成交的资金。
     */
    private void unfreezeRemaining(SpotOrderEntity order, SpotSymbolEntity symbol) {
        BigDecimal remaining = order.getQuantity().subtract(order.getFilledQuantity());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal amount;
        if (SpotOrderSide.BUY.getCode().equals(order.getSide())) {
            amount = calculateQuoteAmount(order.getPrice(), remaining);
        } else {
            amount = remaining;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // 解冻剩余资金，释放占用
        String requestId = "spot-unfreeze-" + order.getId();
        walletClient.unfreeze(order.getUserId(), resolveFreezeAsset(order, symbol), amount, requestId);
    }

    /**
     * 计算冻结金额（买单按价格 * 数量 + 手续费，卖单按数量）。
     */
    private BigDecimal calculateFreezeAmount(SpotOrderEntity order, SpotSymbolEntity symbol) {
        if (SpotOrderSide.BUY.getCode().equals(order.getSide())) {
            return calculateQuoteAmount(order.getPrice(), order.getQuantity());
        }
        return order.getQuantity();
    }

    /**
     * 计算计价币金额（价格 * 数量 + 手续费）。
     */
    private BigDecimal calculateQuoteAmount(BigDecimal price, BigDecimal quantity) {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = price.multiply(quantity);
        BigDecimal fee = BigDecimal.ZERO;
        if (feeRate.compareTo(BigDecimal.ZERO) > 0) {
            fee = base.multiply(feeRate);
        }
        return base.add(fee).setScale(FEE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 确定冻结的资产币种：买单冻结报价币，卖单冻结基础币。
     */
    private String resolveFreezeAsset(SpotOrderEntity order, SpotSymbolEntity symbol) {
        if (SpotOrderSide.BUY.getCode().equals(order.getSide())) {
            if (symbol != null) {
                return symbol.getQuoteAsset();
            }
            return resolveQuoteAsset(order.getSymbol());
        }
        if (symbol != null) {
            return symbol.getBaseAsset();
        }
        return resolveBaseAsset(order.getSymbol());
    }

    /**
     * 解析交易对中的基础币种。
     */
    private String resolveBaseAsset(String symbol) {
        if (symbol == null) {
            return "";
        }
        int index = symbol.indexOf('_');
        if (index > 0) {
            return symbol.substring(0, index);
        }
        return symbol;
    }

    /**
     * 解析交易对中的计价币种。
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
