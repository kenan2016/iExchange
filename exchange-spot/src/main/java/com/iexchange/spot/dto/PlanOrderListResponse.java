package com.iexchange.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 计划委托列表响应。
 *
 * 字段说明：
 * - items 计划单列表
 */
@Schema(name = "PlanOrderListResponse", description = "现货计划委托列表响应")
@Data
public class PlanOrderListResponse {

    @Schema(description = "计划单列表")
    private List<PlanOrderItem> items;

    public static PlanOrderListResponse ok(List<PlanOrderItem> items) {
        PlanOrderListResponse response = new PlanOrderListResponse();
        response.items = items;
        return response;
    }

    /**
     * 计划委托条目。
     *
     * 字段说明：
     * - planOrderId 计划单ID
     * - symbol 交易对
     * - side 方向
     * - type 类型
     * - triggerPrice 触发价格
     * - orderPrice 委托价格
     * - quantity 委托数量
     * - status 状态
     * - triggeredOrderId 触发后订单ID
     * - createdAt 创建时间
     * - triggeredAt 触发时间
     */
    @Schema(name = "PlanOrderItem", description = "计划委托条目")
    @Data
    public static class PlanOrderItem {

        @Schema(description = "计划单ID", example = "20001")
        private Long planOrderId;

        @Schema(description = "交易对", example = "BTC_USDT")
        private String symbol;

        @Schema(description = "方向：BUY/SELL", example = "BUY")
        private String side;

        @Schema(description = "类型：LIMIT/MARKET", example = "LIMIT")
        private String type;

        @Schema(description = "触发价格", example = "30000")
        private BigDecimal triggerPrice;

        @Schema(description = "委托价格", example = "29900")
        private BigDecimal orderPrice;

        @Schema(description = "委托数量", example = "0.1")
        private BigDecimal quantity;

        @Schema(description = "状态", example = "PENDING")
        private String status;

        @Schema(description = "触发后订单ID", example = "10001")
        private Long triggeredOrderId;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;

        @Schema(description = "触发时间")
        private LocalDateTime triggeredAt;
    }
}
