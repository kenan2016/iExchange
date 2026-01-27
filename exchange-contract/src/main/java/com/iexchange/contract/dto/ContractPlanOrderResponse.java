package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 合约计划委托响应。
 *
 * 字段说明：
 * - planOrderId 计划单ID
 * - status 计划单状态（NEW=待触发，TRIGGERED=已触发，CANCELED=已撤销）
 * - triggeredOrderId 触发后订单ID
 */
@Schema(name = "ContractPlanOrderResponse", description = "合约计划委托响应")
@Data
public class ContractPlanOrderResponse {

    @Schema(description = "计划单ID", example = "40001")
    private Long planOrderId;

    @Schema(description = "计划单状态：NEW=待触发，TRIGGERED=已触发，CANCELED=已撤销", example = "NEW")
    private String status;

    @Schema(description = "触发后订单ID", example = "30001")
    private Long triggeredOrderId;

    public static ContractPlanOrderResponse ok(Long planOrderId, String status, Long triggeredOrderId) {
        ContractPlanOrderResponse response = new ContractPlanOrderResponse();
        response.planOrderId = planOrderId;
        response.status = status;
        response.triggeredOrderId = triggeredOrderId;
        return response;
    }
}
