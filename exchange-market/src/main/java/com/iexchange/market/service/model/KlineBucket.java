package com.iexchange.market.service.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * K 线内存桶。
 */
@Data
@AllArgsConstructor
public class KlineBucket {

    /**
     * 交易对。
     */
    private final String symbol;

    /**
     * 起始时间戳（秒）。
     */
    private final long startTime;

    /**
     * 结束时间戳（秒）。
     */
    private final long endTime;

    /**
     * 开盘价。
     */
    private BigDecimal open;

    /**
     * 最高价。
     */
    private BigDecimal high;

    /**
     * 最低价。
     */
    private BigDecimal low;

    /**
     * 收盘价。
     */
    private BigDecimal close;

    /**
     * 成交量。
     */
    private BigDecimal volume;

    /**
     * 更新 K 线桶（高低收与成交量）。
     *
     * 说明：
     * - high/low 取区间内极值
     * - close 始终为最后一笔成交价
     * - volume 为区间累计成交量
     */
    public void apply(BigDecimal price, BigDecimal quantity) {
        if (price.compareTo(high) > 0) {
            high = price;
        }
        if (price.compareTo(low) < 0) {
            low = price;
        }
        close = price;
        volume = volume.add(quantity);
    }
}
