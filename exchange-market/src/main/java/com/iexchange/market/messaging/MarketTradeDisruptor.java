package com.iexchange.market.messaging;

import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.service.DepthService;
import com.iexchange.market.service.KlineService;
import com.iexchange.market.service.MarketTickerService;
import com.iexchange.market.websocket.MarketWebSocketServer;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 行情 Disruptor 管理器。
 */
@Component
public class MarketTradeDisruptor {

    private final MarketTickerService tickerService;
    private final KlineService klineService;
    private final DepthService depthService;
    private final MarketWebSocketServer webSocketServer;
    private final int bufferSize;
    private final int depthLevels;
    private final String klineInterval;

    private Disruptor<MarketTradeEvent> disruptor;
    private RingBuffer<MarketTradeEvent> ringBuffer;
    private MarketTradeEventHandler eventHandler;

    public MarketTradeDisruptor(MarketTickerService tickerService,
                               KlineService klineService,
                               DepthService depthService,
                               MarketWebSocketServer webSocketServer,
                               @Value("${market.trade.buffer-size:1024}") int bufferSize,
                               @Value("${market.websocket.depth-levels:20}") int depthLevels,
                               @Value("${market.websocket.kline-interval:1m}") String klineInterval) {
        this.tickerService = tickerService;
        this.klineService = klineService;
        this.depthService = depthService;
        this.webSocketServer = webSocketServer;
        this.bufferSize = bufferSize;
        this.depthLevels = depthLevels;
        String normalized = klineInterval == null ? "" : klineInterval.trim();
        this.klineInterval = normalized.isEmpty() ? "1m" : normalized;
    }

    /**
     * 初始化 Disruptor 并启动消费线程。
     */
    @PostConstruct
    public void start() {
        int actualSize = normalizeBufferSize(bufferSize);
        this.eventHandler = new MarketTradeEventHandler(
            tickerService,
            klineService,
            depthService,
            webSocketServer,
            depthLevels,
            klineInterval);
        ThreadFactory threadFactory = new MarketThreadFactory();
        this.disruptor = new Disruptor<>(
            new MarketTradeEventFactory(),
            actualSize,
            threadFactory,
            ProducerType.MULTI,
            new BlockingWaitStrategy());
        disruptor.handleEventsWith(eventHandler);
        this.ringBuffer = disruptor.start();
    }

    /**
     * 关闭 Disruptor，释放线程资源。
     */
    @PreDestroy
    public void shutdown() {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    /**
     * 发布成交事件到 Disruptor 队列。
     *
     * 若队列未就绪，直接同步处理，避免丢消息。
     */
    public void publish(SpotTradeEvent event) {
        if (event == null) {
            return;
        }
        if (ringBuffer == null) {
            if (eventHandler != null) {
                eventHandler.process(event);
            }
            return;
        }
        long sequence = ringBuffer.next();
        try {
            MarketTradeEvent slot = ringBuffer.get(sequence);
            slot.setTradeEvent(event);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    /**
     * 将缓冲区大小归一化为 2 的幂。
     */
    private int normalizeBufferSize(int size) {
        int actual = size <= 0 ? 1024 : size;
        int normalized = 1;
        while (normalized < actual) {
            normalized <<= 1;
        }
        return normalized;
    }

    /**
     * 自定义线程工厂，便于定位线程。
     */
    private static class MarketThreadFactory implements ThreadFactory {

        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "market-trade-disruptor-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
