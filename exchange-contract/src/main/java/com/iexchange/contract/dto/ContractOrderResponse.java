package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 合约订单响应。
 *
 * 字段说明：
 * - orderId 订单ID
 * - status 订单状态（NEW=未成交，FILLED=已成交，CANCELED=已撤销）
 * - filledPrice 成交价格（成交后回填）
 * - quantity 委托数量（合约数量）
 */
@Schema(name = "ContractOrderResponse", description = "合约订单响应")
@Data
public class ContractOrderResponse {

    @Schema(description = "订单ID", example = "30001")
    private Long orderId;

    @Schema(description = "订单状态：NEW=未成交，FILLED=已成交，CANCELED=已撤销", example = "NEW")
    private String status;

    @Schema(description = "成交价格（成交后回填）", example = "30000")
    private BigDecimal filledPrice;

    @Schema(description = "委托数量", example = "1")
    private BigDecimal quantity;

    public static ContractOrderResponse ok(Long orderId, String status, BigDecimal filledPrice, BigDecimal quantity) {
        ContractOrderResponse response = new ContractOrderResponse();
        response.orderId = orderId;
        response.status = status;
        response.filledPrice = filledPrice;
        response.quantity = quantity;
        return response;
    }
}
