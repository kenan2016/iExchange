package com.iexchange.spot.dto;

import com.iexchange.spot.entity.SpotOrderEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单查询响应。
 *
 * 字段说明：
 * - orderId 订单ID
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向
 * - type 类型
 * - price 价格
 * - quantity 数量
 * - filledQuantity 已成交数量
 * - status 订单状态
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@Schema(name = "SpotOrderDetailResponse", description = "现货订单详情响应")
@Data
public class SpotOrderDetailResponse {

    @Schema(description = "订单ID", example = "10001")
    private Long orderId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "交易对", example = "BTC_USDT")
    private String symbol;

    @Schema(description = "方向：BUY/SELL", example = "BUY")
    private String side;

    @Schema(description = "类型：LIMIT/MARKET", example = "LIMIT")
    private String type;

    @Schema(description = "价格", example = "30000")
    private BigDecimal price;

    @Schema(description = "数量", example = "0.1")
    private BigDecimal quantity;

    @Schema(description = "已成交数量", example = "0.01")
    private BigDecimal filledQuantity;

    @Schema(description = "订单状态", example = "NEW")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public static SpotOrderDetailResponse ok(SpotOrderEntity order) {
        SpotOrderDetailResponse response = new SpotOrderDetailResponse();
        response.orderId = order.getId();
        response.userId = order.getUserId();
        response.symbol = order.getSymbol();
        response.side = order.getSide();
        response.type = order.getType();
        response.price = order.getPrice();
        response.quantity = order.getQuantity();
        response.filledQuantity = order.getFilledQuantity();
        response.status = order.getStatus();
        response.createdAt = order.getCreatedAt();
        response.updatedAt = order.getUpdatedAt();
        return response;
    }
}
