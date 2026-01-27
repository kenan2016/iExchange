package com.iexchange.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 下单请求。
 *
 * 字段说明：
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向：BUY/SELL
 * - type 类型：LIMIT/MARKET
 * - quantity 委托数量
 * - price 价格（市价单为保护价）
 */
@Schema(name = "PlaceOrderRequest", description = "现货下单请求")
@Data
public class PlaceOrderRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 交易对。
     */
    @Schema(description = "交易对", example = "BTC_USDT")
    @NotBlank(message = "交易对不能为空")
    private String symbol;

    /**
     * 方向：BUY/SELL。
     */
    @Schema(description = "方向：BUY/SELL", example = "BUY")
    @NotBlank(message = "订单方向不能为空")
    private String side;

    /**
     * 类型：LIMIT/MARKET。
     */
    @Schema(description = "类型：LIMIT/MARKET", example = "LIMIT")
    @NotBlank(message = "订单类型不能为空")
    private String type;

    /**
     * 委托数量。
     */
    @Schema(description = "委托数量", example = "0.1")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.00000001", message = "数量必须大于0")
    private BigDecimal quantity;

    /**
     * 限价单必填，市价单需提供保护价格。
     */
    @Schema(description = "委托价格（市价单为保护价）", example = "30000")
    private BigDecimal price;
}
