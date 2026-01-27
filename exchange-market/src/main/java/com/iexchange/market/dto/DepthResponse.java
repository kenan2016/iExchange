package com.iexchange.market.dto;

import com.iexchange.market.service.model.DepthLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 深度响应。
 */
@Schema(name = "DepthResponse", description = "盘口深度响应")
@Data
public class DepthResponse {

    /**
     * 交易对。
     */
    @Schema(description = "交易对", example = "BTC_USDT")
    private String symbol;

    /**
     * 买盘档位列表。
     */
    @Schema(description = "买盘档位列表")
    private List<DepthLevel> bids;

    /**
     * 卖盘档位列表。
     */
    @Schema(description = "卖盘档位列表")
    private List<DepthLevel> asks;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public static DepthResponse ok(String symbol, List<DepthLevel> bids, List<DepthLevel> asks, LocalDateTime updateTime) {
        DepthResponse response = new DepthResponse();
        response.symbol = symbol;
        response.bids = bids;
        response.asks = asks;
        response.updateTime = updateTime;
        return response;
    }
}
