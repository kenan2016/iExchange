package com.iexchange.market.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * K 线文档。
 */
@Document("kline")
@Data
public class KlineDocument {

    /**
     * 文档 ID。
     */
    @Id
    private String id;

    /**
     * 交易对。
     */
    private String symbol;

    /**
     * 秒级时间戳，表示该 K 线起始时间。
     */
    private long startTime;

    /**
     * 秒级结束时间戳。
     */
    private long endTime;

    /**
     * 周期（例如 1m/5m/15m/1h）。
     */
    private String interval;

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
     * 入库时间。
     */
    private LocalDateTime createdAt;


}
