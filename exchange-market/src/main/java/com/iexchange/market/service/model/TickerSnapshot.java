package com.iexchange.market.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Ticker 快照。
 */
@Data
public class TickerSnapshot {

    /**
     * 交易对。
     */
    private String symbol;

    /**
     * 最新成交价。
     */
    private BigDecimal lastPrice;

    /**
     * 成交量。
     */
    private BigDecimal volume;

    /**
     * 最新成交时间。
     */
    private LocalDateTime lastTradeTime;


}
