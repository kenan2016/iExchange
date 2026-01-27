package com.iexchange.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 下单/撤单响应。
 *
 * 字段说明：
 * - orderId 订单ID
 * - status 订单状态
 * - filledQuantity 已成交数量
 * - remainingQuantity 剩余数量
 */
@Schema(name = "SpotOrderResponse", description = "现货下单/撤单响应")
@Data
public class SpotOrderResponse {

    @Schema(description = "订单ID", example = "10001")
    private Long orderId;

    @Schema(description = "订单状态", example = "NEW")
    private String status;

    @Schema(description = "已成交数量", example = "0.01")
    private BigDecimal filledQuantity;

    @Schema(description = "剩余数量", example = "0.09")
    private BigDecimal remainingQuantity;

    public static SpotOrderResponse ok(Long orderId, String status, BigDecimal filledQuantity, BigDecimal remainingQuantity) {
        SpotOrderResponse response = new SpotOrderResponse();
        response.orderId = orderId;
        response.status = status;
        response.filledQuantity = filledQuantity;
        response.remainingQuantity = remainingQuantity;
        return response;
    }
}
