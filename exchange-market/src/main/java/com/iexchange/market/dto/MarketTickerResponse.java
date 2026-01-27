package com.iexchange.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 行情 Ticker 响应。
 */
@Schema(name = "MarketTickerResponse", description = "行情Ticker响应")
@Data
public class MarketTickerResponse {

    /**
     * 交易对。
     */
    @Schema(description = "交易对", example = "BTC_USDT")
    private String symbol;

    /**
     * 最新成交价。
     */
    @Schema(description = "最新成交价", example = "30000")
    private BigDecimal lastPrice;

    /**
     * 24h 成交量（示例取累计量）。
     */
    @Schema(description = "成交量", example = "100")
    private BigDecimal volume;

    /**
     * 最新成交时间。
     */
    @Schema(description = "最新成交时间")
    private LocalDateTime lastTradeTime;

    public static MarketTickerResponse ok(String symbol, BigDecimal lastPrice,
                                          BigDecimal volume, LocalDateTime lastTradeTime) {
        MarketTickerResponse response = new MarketTickerResponse();
        response.symbol = symbol;
        response.lastPrice = lastPrice;
        response.volume = volume;
        response.lastTradeTime = lastTradeTime;
        return response;
    }
}
