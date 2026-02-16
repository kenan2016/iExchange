package com.iexchange.spot.engine;

import com.iexchange.spot.client.WalletAccountClient;
import com.iexchange.spot.dto.SpotTradeEvent;
import com.iexchange.spot.entity.SpotOrderEntity;
import com.iexchange.spot.entity.SpotTradeEntity;
import com.iexchange.spot.enums.SpotOrderSide;
import com.iexchange.spot.enums.SpotOrderStatus;
import com.iexchange.spot.enums.SpotOrderType;
import com.iexchange.spot.mapper.SpotOrderMapper;
import com.iexchange.spot.mapper.SpotTradeMapper;
import com.iexchange.spot.messaging.SpotTradeEventProducer;
import com.iexchange.spot.service.SpotFeeService;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 现货撮合引擎（简化版）。
 *
 * 核心职责：
 * - 接收下单/撤单事件
 * - 维护每个交易对的订单簿
 * - 按价格优先、时间优先完成撮合
 * - 生成成交、更新订单状态、结算资金
 */
@Slf4j
@Service
public class SpotMatchingEngine {
    private static final int FEE_SCALE = 8;
    /**
     * 交易对 -> 订单簿（每个交易对独立撮合，不互相干扰）。
     */
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final SpotOrderMapper orderMapper;
    private final SpotTradeMapper tradeMapper;
    private final SpotTradeEventProducer tradeEventProducer;
    private final SpotFeeService feeService;
    private final WalletAccountClient walletClient;
    private final Disruptor<SpotMatchingEvent> disruptor;
    /**
     * 撮合与落库使用事务模板，保证订单/成交一致性（简化示意）。
     */
    private final TransactionTemplate transactionTemplate;
    /**
     * 等待撮合结果的超时时间，避免接口阻塞过久。
     */
    private final long awaitTimeoutMs;
    /**
     * 手续费费率（成交额 * 费率），示例用途。
     */
    private final BigDecimal feeRate;
    private RingBuffer<SpotMatchingEvent> ringBuffer;

    public SpotMatchingEngine(SpotOrderMapper orderMapper,
                              SpotTradeMapper tradeMapper,
                              SpotTradeEventProducer tradeEventProducer,
                              SpotFeeService feeService,
                              WalletAccountClient walletClient,
                              PlatformTransactionManager transactionManager,
                              @Value("${spot.matching.buffer-size:1024}") int bufferSize,
                              @Value("${spot.matching.await-timeout-ms:3000}") long awaitTimeoutMs,
                              @Value("${spot.fee.rate:0.001}") BigDecimal feeRate) {
        this.orderMapper = orderMapper;
        this.tradeMapper = tradeMapper;
        this.tradeEventProducer = tradeEventProducer;
        this.feeService = feeService;
        this.walletClient = walletClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.awaitTimeoutMs = awaitTimeoutMs;
        this.feeRate = feeRate == null ? BigDecimal.ZERO : feeRate;
        int normalizedSize = normalizeBufferSize(bufferSize);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("spot-matching-disruptor");
            thread.setDaemon(true);
            return thread;
        };
        // Disruptor 解耦业务线程与撮合线程，提升吞吐并降低锁竞争
        this.disruptor = new Disruptor<>(
            new SpotMatchingEventFactory(),
            normalizedSize,
            threadFactory,
            ProducerType.MULTI,
            new BlockingWaitStrategy());
        this.disruptor.handleEventsWith(new SpotMatchingEventHandler());
    }

    /**
     * 启动撮合引擎并初始化 Disruptor。
     */
    @PostConstruct
    public void start() {
        this.ringBuffer = disruptor.start();
        log.info("撮合引擎 Disruptor 已启动，bufferSize={}", ringBuffer.getBufferSize());
    }

    /**
     * 关闭撮合引擎，释放线程与缓冲区资源。
     */
    @PreDestroy
    public void shutdown() {
        disruptor.shutdown();
    }

    /**
     * 撮合订单（价格优先 + 时间优先）。
     */
    public SpotOrderEntity match(SpotOrderEntity takerOrder) {
        SpotMatchingResult result = publishAndAwait(SpotMatchingEventType.MATCH, takerOrder);
        return result.getOrder() == null ? takerOrder : result.getOrder();
    }

    /**
     * 撤销订单。
     */
    public boolean cancel(SpotOrderEntity order) {
        SpotMatchingResult result = publishAndAwait(SpotMatchingEventType.CANCEL, order);
        return result.isCanceled();
    }

    /**
     * 发布撮合/撤单事件并等待处理结果。
     *
     * 说明：
     * - 使用 Disruptor 解耦业务线程与撮合线程
     * - 通过 CompletableFuture 回传处理结果
     */
    private SpotMatchingResult publishAndAwait(SpotMatchingEventType type, SpotOrderEntity order) {
        if (ringBuffer == null) {
            throw new IllegalStateException("撮合引擎尚未启动");
        }
        // 通过 Disruptor 投递撮合事件
        CompletableFuture<SpotMatchingResult> future = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
            SpotMatchingEvent event = ringBuffer.get(sequence);
            event.setType(type);
            event.setOrder(order);
            event.setFuture(future);
        } finally {
            ringBuffer.publish(sequence);
        }
        return awaitResult(order, future);
    }

    /**
     * 等待撮合结果，超时则返回当前订单状态，避免请求长期阻塞。
     */
    private SpotMatchingResult awaitResult(SpotOrderEntity order, CompletableFuture<SpotMatchingResult> future) {
        try {
            return future.get(awaitTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            SpotMatchingResult fallback = new SpotMatchingResult();
            fallback.setOrder(order);
            log.warn("等待撮合结果超时或失败，将返回当前订单状态", ex);
            return fallback;
        }
    }

    /**
     * 撮合入口，根据买卖方向选择不同的撮合路径。
     *
     * 说明：
     * - 买单从卖盘最优价开始吃单
     * - 卖单从买盘最优价开始吃单
     */
    private SpotOrderEntity processMatch(SpotOrderEntity takerOrder) {
        // 每个交易对维护独立订单簿
        OrderBook orderBook = orderBooks.computeIfAbsent(takerOrder.getSymbol(), symbol -> new OrderBook());
        if (SpotOrderSide.BUY.getCode().equals(takerOrder.getSide())) {
            matchBuy(takerOrder, orderBook);
        } else {
            matchSell(takerOrder, orderBook);
        }
        return takerOrder;
    }

    /**
     * 撤单处理，从订单簿移除指定订单。
     */
    private boolean processCancel(SpotOrderEntity order) {
        OrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) {
            return false;
        }
        return orderBook.cancel(order.getId());
    }

    /**
     * Disruptor 需要 2 的幂大小，这里做归一化。
     */
    private int normalizeBufferSize(int bufferSize) {
        int size = 1;
        int target = Math.max(2, bufferSize);
        while (size < target) {
            size <<= 1;
        }
        return size;
    }

    /**
     * 买单撮合：从卖盘最优价开始吃单，价格优先、时间优先。
     *
     * 关键点：
     * - 只要剩余数量大于 0，就持续吃单
     * - 市价单通过“保护价”限制吃单深度
     */
    private void matchBuy(SpotOrderEntity takerOrder, OrderBook orderBook) {
        BigDecimal remaining = remaining(takerOrder);
        boolean priceProtected = isPriceProtected(takerOrder);
        // 买单从卖盘最优价开始撮合
        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            OrderBookEntry bestSell = orderBook.peekBestSell();
            if (bestSell == null) {
                break;
            }
            BigDecimal makerPrice = bestSell.getOrder().getPrice();
            // 市价单按保护价限制吃单深度
            if (priceProtected && makerPrice.compareTo(takerOrder.getPrice()) > 0) {
                break;
            }
            BigDecimal tradeQuantity = remaining.min(bestSell.getRemainingQuantity());
            remaining = remaining.subtract(tradeQuantity);
            handleTrade(takerOrder, bestSell, tradeQuantity, makerPrice, orderBook);
        }
        finalizeTaker(takerOrder, remaining, orderBook);
    }

    /**
     * 卖单撮合：从买盘最优价开始吃单，价格优先、时间优先。
     *
     * 关键点：
     * - 只要剩余数量大于 0，就持续吃单
     * - 市价单通过“保护价”限制吃单深度
     */
    private void matchSell(SpotOrderEntity takerOrder, OrderBook orderBook) {
        BigDecimal remaining = remaining(takerOrder);
        boolean priceProtected = isPriceProtected(takerOrder);
        // 卖单从买盘最优价开始撮合
        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            OrderBookEntry bestBuy = orderBook.peekBestBuy();
            if (bestBuy == null) {
                break;
            }
            BigDecimal makerPrice = bestBuy.getOrder().getPrice();
            // 市价单按保护价限制吃单深度
            if (priceProtected && makerPrice.compareTo(takerOrder.getPrice()) < 0) {
                break;
            }
            BigDecimal tradeQuantity = remaining.min(bestBuy.getRemainingQuantity());
            remaining = remaining.subtract(tradeQuantity);
            handleTrade(takerOrder, bestBuy, tradeQuantity, makerPrice, orderBook);
        }
        finalizeTaker(takerOrder, remaining, orderBook);
    }

    /**
     * 处理撮合成交。
     *
     * 主要步骤：
     * 1) 更新 maker 订单剩余与状态
     * 2) 生成成交记录并落库
     * 3) 计算费用/结算资金
     * 4) 推送成交事件给行情服务
     */
    private void handleTrade(SpotOrderEntity takerOrder, OrderBookEntry makerEntry,
                             BigDecimal tradeQuantity, BigDecimal tradePrice, OrderBook orderBook) {
        // 更新 maker 订单剩余数量
        makerEntry.reduce(tradeQuantity);
        SpotOrderEntity makerOrder = makerEntry.getOrder();
        makerOrder.setFilledQuantity(makerOrder.getFilledQuantity().add(tradeQuantity));
        if (makerOrder.getFilledQuantity().compareTo(makerOrder.getQuantity()) >= 0) {
            makerOrder.setStatus(SpotOrderStatus.FILLED.getCode());
            orderBook.finishEntry(makerEntry);
        } else {
            makerOrder.setStatus(SpotOrderStatus.PARTIAL_FILLED.getCode());
        }
        makerOrder.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(makerOrder);

        // 生成成交记录并落库
        SpotTradeEntity trade = buildTrade(takerOrder, makerOrder, tradeQuantity, tradePrice);
        tradeMapper.insert(trade);
        SpotOrderEntity buyOrder = SpotOrderSide.BUY.getCode().equals(takerOrder.getSide()) ? takerOrder : makerOrder;
        SpotOrderEntity sellOrder = SpotOrderSide.SELL.getCode().equals(takerOrder.getSide()) ? takerOrder : makerOrder;
        // 计算手续费与资金结算
        feeService.recordTradeFee(trade, buyOrder, sellOrder);
        settleWallet(trade, buyOrder, sellOrder);
        // 成交事件投递给行情服务
        tradeEventProducer.sendTradeEvent(buildTradeEvent(trade));
    }

    /**
     * 处理吃单结果：更新订单状态，并将剩余挂单或解冻。
     *
     * 规则：
     * - 限价单：剩余数量进入订单簿
     * - 市价单：剩余数量直接解冻
     */
    private void finalizeTaker(SpotOrderEntity takerOrder, BigDecimal remaining, OrderBook orderBook) {
        BigDecimal filled = takerOrder.getQuantity().subtract(remaining);
        takerOrder.setFilledQuantity(filled);
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            takerOrder.setStatus(SpotOrderStatus.FILLED.getCode());
        } else if (SpotOrderType.LIMIT.getCode().equals(takerOrder.getType())) {
            if (filled.compareTo(BigDecimal.ZERO) > 0) {
                takerOrder.setStatus(SpotOrderStatus.PARTIAL_FILLED.getCode());
            } else {
                takerOrder.setStatus(SpotOrderStatus.NEW.getCode());
            }
            // 限价单剩余挂入订单簿
            orderBook.add(takerOrder, remaining);
        } else {
            if (filled.compareTo(BigDecimal.ZERO) > 0) {
                takerOrder.setStatus(SpotOrderStatus.PARTIAL_FILLED.getCode());
            } else {
                takerOrder.setStatus(SpotOrderStatus.CANCELED.getCode());
            }
            // 市价单剩余解冻资金
            unfreezeMarketRemaining(takerOrder, remaining);
        }
        takerOrder.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(takerOrder);
    }

    /**
     * 计算订单剩余数量。
     */
    private BigDecimal remaining(SpotOrderEntity order) {
        BigDecimal filled = order.getFilledQuantity() == null ? BigDecimal.ZERO : order.getFilledQuantity();
        return order.getQuantity().subtract(filled);
    }

    /**
     * 构建成交记录实体。
     */
    private SpotTradeEntity buildTrade(SpotOrderEntity takerOrder, SpotOrderEntity makerOrder,
                                       BigDecimal quantity, BigDecimal price) {
        SpotTradeEntity trade = new SpotTradeEntity();
        trade.setSymbol(takerOrder.getSymbol());
        if (SpotOrderSide.BUY.getCode().equals(takerOrder.getSide())) {
            trade.setBuyOrderId(takerOrder.getId());
            trade.setSellOrderId(makerOrder.getId());
        } else {
            trade.setBuyOrderId(makerOrder.getId());
            trade.setSellOrderId(takerOrder.getId());
        }
        trade.setPrice(price);
        trade.setQuantity(quantity);
        trade.setTakerSide(takerOrder.getSide());
        trade.setCreatedAt(LocalDateTime.now());
        return trade;
    }

    /**
     * 构建成交事件（发送给行情服务）。
     */
    private SpotTradeEvent buildTradeEvent(SpotTradeEntity trade) {
        SpotTradeEvent event = new SpotTradeEvent();
        event.setSymbol(trade.getSymbol());
        event.setBuyOrderId(trade.getBuyOrderId());
        event.setSellOrderId(trade.getSellOrderId());
        event.setPrice(trade.getPrice());
        event.setQuantity(trade.getQuantity());
        event.setTakerSide(trade.getTakerSide());
        event.setTradeTime(trade.getCreatedAt());
        return event;
    }

    /**
     * 判断是否启用了价格保护（用于市价单保护价）。
     */
    private boolean isPriceProtected(SpotOrderEntity order) {
        return order.getPrice() != null && order.getPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 市价单未完全成交时解冻剩余资金。
     */
    private void unfreezeMarketRemaining(SpotOrderEntity order, BigDecimal remaining) {
        if (remaining == null || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (SpotOrderSide.BUY.getCode().equals(order.getSide())) {
            BigDecimal amount = calculateBuyerReserved(order.getPrice(), remaining);
            // 买单解冻报价币
            walletClient.unfreeze(order.getUserId(), resolveQuoteAsset(order.getSymbol()),
                amount, "spot-market-unfreeze-" + order.getId());
        } else {
            // 卖单解冻基础币
            walletClient.unfreeze(order.getUserId(), resolveBaseAsset(order.getSymbol()),
                remaining, "spot-market-unfreeze-" + order.getId());
        }
    }

    /**
     * 成交后资金结算。
     */
    private void settleWallet(SpotTradeEntity trade, SpotOrderEntity buyOrder, SpotOrderEntity sellOrder) {
        if (trade == null || buyOrder == null || sellOrder == null) {
            return;
        }
        BigDecimal tradeQuantity = trade.getQuantity();
        BigDecimal tradePrice = trade.getPrice();
        String baseAsset = resolveBaseAsset(trade.getSymbol());
        String quoteAsset = resolveQuoteAsset(trade.getSymbol());

        try {
            // 买方：扣减冻结的报价币，多余部分解冻；入账基础币
            BigDecimal buyerReserved = calculateBuyerReserved(buyOrder.getPrice(), tradeQuantity);
            BigDecimal buyerActual = calculateBuyerReserved(tradePrice, tradeQuantity);
            walletClient.deductFrozen(buyOrder.getUserId(), quoteAsset, buyerActual,
                "spot-trade-deduct-" + trade.getId() + "-buy");
            if (buyerReserved.compareTo(buyerActual) > 0) {
                walletClient.unfreeze(buyOrder.getUserId(), quoteAsset, buyerReserved.subtract(buyerActual),
                    "spot-trade-unfreeze-" + trade.getId() + "-buy");
            }
            walletClient.tradeIn(buyOrder.getUserId(), baseAsset, tradeQuantity,
                "spot-trade-credit-" + trade.getId() + "-buy");
        } catch (Exception ex) {
            log.warn("买方资金结算失败，tradeId={}", trade.getId(), ex);
        }

        try {
            // 卖方：扣减冻结的基础币，入账报价币（扣手续费）
            walletClient.deductFrozen(sellOrder.getUserId(), baseAsset, tradeQuantity,
                "spot-trade-deduct-" + trade.getId() + "-sell");
            BigDecimal feeAmount = calculateFeeAmount(tradePrice, tradeQuantity);
            BigDecimal proceeds = tradePrice.multiply(tradeQuantity).subtract(feeAmount);
            if (proceeds.compareTo(BigDecimal.ZERO) > 0) {
                walletClient.tradeIn(sellOrder.getUserId(), quoteAsset, proceeds,
                    "spot-trade-credit-" + trade.getId() + "-sell");
            }
        } catch (Exception ex) {
            log.warn("卖方资金结算失败，tradeId={}", trade.getId(), ex);
        }
    }

    /**
     * 计算买方占用金额（含手续费），用于冻结与结算。
     */
    private BigDecimal calculateBuyerReserved(BigDecimal price, BigDecimal quantity) {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal notional = price.multiply(quantity);
        if (feeRate.compareTo(BigDecimal.ZERO) > 0) {
            notional = notional.add(notional.multiply(feeRate));
        }
        return notional.setScale(FEE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算手续费金额（按成交额 * 费率）。
     */
    private BigDecimal calculateFeeAmount(BigDecimal price, BigDecimal quantity) {
        if (feeRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return price.multiply(quantity).multiply(feeRate).setScale(FEE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 解析交易对中的基础币种（如 BTC_USDT -> BTC）。
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
     * 解析交易对中的计价币种（如 BTC_USDT -> USDT）。
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

    /**
     * 撮合事件处理器。
     *
     * 说明：
     * - Disruptor 线程中执行撮合，避免阻塞业务线程
     * - 匹配逻辑可被事务包裹，确保订单/成交原子性（示意）
     */
    private class SpotMatchingEventHandler implements EventHandler<SpotMatchingEvent> {

        /**
         * Disruptor 事件消费入口，负责真正撮合/撤单处理。
         */
        @Override
        public void onEvent(SpotMatchingEvent event, long sequence, boolean endOfBatch) {
            if (event.getType() == null) {
                return;
            }
            try {
                SpotMatchingResult result = new SpotMatchingResult();
                if (event.getType() == SpotMatchingEventType.MATCH) {
                    transactionTemplate.executeWithoutResult(status -> {
                        SpotOrderEntity updated = processMatch(event.getOrder());
                        result.setOrder(updated);
                    });
                } else if (event.getType() == SpotMatchingEventType.CANCEL) {
                    boolean canceled = processCancel(event.getOrder());
                    result.setCanceled(canceled);
                }
                event.getFuture().complete(result);
            } catch (Exception ex) {
                event.getFuture().completeExceptionally(ex);
                log.warn("撮合事件处理失败", ex);
            } finally {
                event.reset();
            }
        }
    }
}
