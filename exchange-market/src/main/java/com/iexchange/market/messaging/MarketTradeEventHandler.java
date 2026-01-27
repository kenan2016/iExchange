package com.iexchange.market.messaging;

import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.service.DepthService;
import com.iexchange.market.service.KlineService;
import com.iexchange.market.service.MarketTickerService;
import com.iexchange.market.websocket.MarketWebSocketServer;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 行情事件处理器。
 */
@Slf4j
public class MarketTradeEventHandler implements EventHandler<MarketTradeEvent> {

    private final MarketTickerService tickerService;
    private final KlineService klineService;
    private final DepthService depthService;
    private final MarketWebSocketServer webSocketServer;
    private final int depthLevels;
    private final String klineInterval;

    public MarketTradeEventHandler(MarketTickerService tickerService,
                                   KlineService klineService,
                                   DepthService depthService,
                                   MarketWebSocketServer webSocketServer,
                                   int depthLevels,
                                   String klineInterval) {
        this.tickerService = tickerService;
        this.klineService = klineService;
        this.depthService = depthService;
        this.webSocketServer = webSocketServer;
        this.depthLevels = depthLevels;
        this.klineInterval = klineInterval;
    }

    /**
     * Disruptor 事件消费入口，串行处理成交事件。
     */
    @Override
    public void onEvent(MarketTradeEvent event, long sequence, boolean endOfBatch) {
        SpotTradeEvent tradeEvent = event.getTradeEvent();
        if (tradeEvent == null) {
            return;
        }
        process(tradeEvent);
        event.clear();
    }

    /**
     * 同步处理成交事件，便于复用。
     */
    public void process(SpotTradeEvent event) {
        try {
            // 成交事件进入行情主干：ticker、kline、depth
            tickerService.onTrade(event);
            klineService.onTrade(event);
            depthService.onTrade(event);
            // 推送到 WebSocket（trade/ticker/depth/kline）
            webSocketServer.broadcastTrade(event);
            webSocketServer.broadcastTicker(tickerService.getTicker(event.getSymbol()));
            webSocketServer.broadcastDepth(depthService.getDepth(event.getSymbol(), depthLevels));
            webSocketServer.broadcastKline(event.getSymbol(), klineInterval, resolveLatestKline(event.getSymbol()));
        } catch (Exception ex) {
            log.warn("行情事件处理失败", ex);
        }
    }

    /**
     * 获取最新一根 K 线，用于推送。
     */
    private com.iexchange.market.document.KlineDocument resolveLatestKline(String symbol) {
        try {
            java.util.List<com.iexchange.market.document.KlineDocument> items =
                klineService.query(symbol, klineInterval, 1);
            if (items == null || items.isEmpty()) {
                return null;
            }
            return items.get(0);
        } catch (Exception ex) {
            log.warn("获取最新K线失败", ex);
            return null;
        }
    }
}
