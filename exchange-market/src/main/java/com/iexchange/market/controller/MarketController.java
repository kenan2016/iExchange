package com.iexchange.market.controller;

import com.iexchange.common.response.R;
import com.iexchange.market.document.KlineDocument;
import com.iexchange.market.dto.DepthResponse;
import com.iexchange.market.dto.KlineQueryResponse;
import com.iexchange.market.dto.MarketTickerResponse;
import com.iexchange.market.service.DepthService;
import com.iexchange.market.service.KlineService;
import com.iexchange.market.service.MarketTickerService;
import com.iexchange.market.service.model.DepthSnapshot;
import com.iexchange.market.service.model.TickerSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行情接口。
 */
@Tag(name = "行情接口", description = "行情与K线查询接口")
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketTickerService tickerService;
    private final KlineService klineService;
    private final DepthService depthService;

    public MarketController(MarketTickerService tickerService, KlineService klineService, DepthService depthService) {
        this.tickerService = tickerService;
        this.klineService = klineService;
        this.depthService = depthService;
    }

    /**
     * 查询最新行情（Ticker）。
     */
    @Operation(summary = "查询最新行情", description = "按交易对查询最新行情")
    @GetMapping("/ticker")
    public R<MarketTickerResponse> ticker(@Parameter(description = "交易对", required = true)
                                          @RequestParam("symbol") String symbol) {
        TickerSnapshot snapshot = tickerService.getTicker(symbol);
        if (snapshot == null) {
            return R.fail("暂无行情数据");
        }
        MarketTickerResponse response = MarketTickerResponse.ok(
            symbol, snapshot.getLastPrice(), snapshot.getVolume(), snapshot.getLastTradeTime());
        return R.ok("查询成功", response);
    }

    /**
     * 查询 K 线数据。
     */
    @Operation(summary = "查询K线", description = "按周期查询K线列表")
    @GetMapping("/klines")
    public R<KlineQueryResponse> klines(@Parameter(description = "交易对", required = true)
                                        @RequestParam("symbol") String symbol,
                                        @Parameter(description = "周期，例如 1m/5m/1h", required = true)
                                        @RequestParam(value = "interval", defaultValue = "1m") String interval,
                                        @Parameter(description = "返回条数", required = true)
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        try {
            List<KlineDocument> documents = klineService.query(symbol, interval, limit);
            // 将文档模型转换为 API 输出结构，避免直接暴露存储结构
            List<KlineQueryResponse.KlineItem> items = new ArrayList<>();
            for (KlineDocument document : documents) {
                KlineQueryResponse.KlineItem item = new KlineQueryResponse.KlineItem();
                item.setStartTime(document.getStartTime());
                item.setEndTime(document.getEndTime());
                item.setOpen(document.getOpen());
                item.setHigh(document.getHigh());
                item.setLow(document.getLow());
                item.setClose(document.getClose());
                item.setVolume(document.getVolume());
                item.setCreatedAt(document.getCreatedAt());
                items.add(item);
            }
            KlineQueryResponse response = KlineQueryResponse.ok(symbol, interval, items);
            return R.ok("查询成功", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 查询盘口深度。
     */
    @Operation(summary = "查询深度", description = "按交易对查询盘口深度")
    @GetMapping("/depth")
    public R<DepthResponse> depth(@Parameter(description = "交易对", required = true)
                                  @RequestParam("symbol") String symbol,
                                  @Parameter(description = "档位数量", required = true)
                                  @RequestParam(value = "limit", defaultValue = "20") int limit) {
        DepthSnapshot snapshot = depthService.getDepth(symbol, limit);
        if (snapshot == null) {
            return R.fail("暂无深度数据");
        }
        DepthResponse response = DepthResponse.ok(
            symbol, snapshot.getBids(), snapshot.getAsks(), snapshot.getUpdateTime());
        return R.ok("查询成功", response);
    }
}
