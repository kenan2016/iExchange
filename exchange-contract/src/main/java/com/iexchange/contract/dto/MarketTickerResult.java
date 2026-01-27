package com.iexchange.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 行情 Ticker 响应（合约端解析用，示例）。
 *
 * 字段说明：
 * - symbol 交易对
 * - lastPrice 最新成交价（最近一笔成交价格）
 * - volume 成交量（一定时间内的累计成交量）
 * - lastTradeTime 最新成交时间
 */
@Data
public class MarketTickerResult {

    /**
     * 交易对。
     */
    private String symbol;
    /**
     * 最新成交价（撮合最新一笔的成交价格）。
     */
    private BigDecimal lastPrice;
    /**
     * 成交量（某个统计周期内的累计成交量）。
     */
    private BigDecimal volume;
    /**
     * 最新成交时间。
     */
    private LocalDateTime lastTradeTime;
}
