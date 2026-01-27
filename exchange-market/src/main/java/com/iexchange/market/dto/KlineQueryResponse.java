package com.iexchange.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * K 线查询响应。
 */
@Schema(name = "KlineQueryResponse", description = "K线查询响应")
@Data
public class KlineQueryResponse {

    /**
     * 交易对。
     */
    @Schema(description = "交易对", example = "BTC_USDT")
    private String symbol;

    /**
     * 周期（例如 1m/5m）。
     */
    @Schema(description = "周期", example = "1m")
    private String interval;

    /**
     * K 线条目列表。
     */
    @Schema(description = "K线条目列表")
    private List<KlineItem> items;

    public static KlineQueryResponse ok(String symbol, String interval, List<KlineItem> items) {
        KlineQueryResponse response = new KlineQueryResponse();
        response.symbol = symbol;
        response.interval = interval;
        response.items = items;
        return response;
    }



    /**
     * K 线返回条目。
     */
    @Schema(name = "KlineItem", description = "K线条目")
    @Data
    public static class KlineItem {

        /**
         * 起始时间戳（秒）。
         */
        @Schema(description = "起始时间戳（秒）", example = "1700000000")
        private long startTime;

        /**
         * 结束时间戳（秒）。
         */
        @Schema(description = "结束时间戳（秒）", example = "1700000060")
        private long endTime;

        /**
         * 开盘价。
         */
        @Schema(description = "开盘价", example = "30000")
        private BigDecimal open;

        /**
         * 最高价。
         */
        @Schema(description = "最高价", example = "30100")
        private BigDecimal high;

        /**
         * 最低价。
         */
        @Schema(description = "最低价", example = "29900")
        private BigDecimal low;

        /**
         * 收盘价。
         */
        @Schema(description = "收盘价", example = "30050")
        private BigDecimal close;

        /**
         * 成交量。
         */
        @Schema(description = "成交量", example = "12.34")
        private BigDecimal volume;

        /**
         * 入库时间。
         */
        @Schema(description = "入库时间")
        private LocalDateTime createdAt;
    }
}
