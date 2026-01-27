package com.iexchange.spot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 行情服务返回的 Ticker 结构。
 *
 * 字段说明：
 * - symbol 交易对
 * - lastPrice 最新成交价
 * - volume 成交量
 * - lastTradeTime 最新成交时间
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketTickerResult {

    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal volume;
    private LocalDateTime lastTradeTime;
}
