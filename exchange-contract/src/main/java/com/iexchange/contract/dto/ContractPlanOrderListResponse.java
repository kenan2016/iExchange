package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 合约计划委托列表响应。
 *
 * 字段说明：
 * - items 计划单列表
 */
@Schema(name = "ContractPlanOrderListResponse", description = "合约计划委托列表响应")
@Data
public class ContractPlanOrderListResponse {

    @Schema(description = "计划单列表")
    private List<PlanOrderItem> items;

    public static ContractPlanOrderListResponse ok(List<PlanOrderItem> items) {
        ContractPlanOrderListResponse response = new ContractPlanOrderListResponse();
        response.items = items;
        return response;
    }

    /**
     * 计划委托条目。
     *
     * 字段说明：
     * - planOrderId 计划单ID
     * - symbol 交易对
     * - action 开平动作
     * - side 方向
     * - type 类型
     * - triggerPrice 触发价格
     * - orderPrice 委托价格
     * - quantity 委托数量
     * - leverage 杠杆倍数
     * - marginMode 保证金模式
     * - status 状态
     * - triggeredOrderId 触发后订单ID
     * - createdAt 创建时间
     * - triggeredAt 触发时间
     */
    @Schema(name = "ContractPlanOrderItem", description = "合约计划委托条目")
    @Data
    public static class PlanOrderItem {

        @Schema(description = "计划单ID", example = "40001")
        private Long planOrderId;

        @Schema(description = "交易对", example = "BTCUSDT-PERP")
        private String symbol;

        @Schema(description = "开平动作：OPEN=开仓，CLOSE=平仓", example = "OPEN")
        private String action;

        @Schema(description = "方向：LONG=看涨做多，SHORT=看跌做空", example = "LONG")
        private String side;

        @Schema(description = "类型：LIMIT=限价，MARKET=市价", example = "LIMIT")
        private String type;

        @Schema(description = "触发价格（达到该价格时触发）", example = "30000")
        private BigDecimal triggerPrice;

        @Schema(description = "委托价格（触发后下单价格）", example = "29900")
        private BigDecimal orderPrice;

        @Schema(description = "委托数量（合约数量）", example = "1")
        private BigDecimal quantity;

        @Schema(description = "杠杆倍数（开仓使用）", example = "10")
        private Integer leverage;

        @Schema(description = "保证金模式：CROSS=全仓，ISOLATED=逐仓", example = "CROSS")
        private String marginMode;

        @Schema(description = "状态：NEW=待触发，TRIGGERED=已触发，CANCELED=已撤销", example = "NEW")
        private String status;

        @Schema(description = "触发后订单ID", example = "30001")
        private Long triggeredOrderId;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;

        @Schema(description = "触发时间")
        private LocalDateTime triggeredAt;
    }
}
