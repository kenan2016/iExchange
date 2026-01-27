package com.iexchange.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 计划委托响应。
 *
 * 字段说明：
 * - planOrderId 计划单ID
 * - status 计划单状态
 * - triggeredOrderId 触发后订单ID
 */
@Schema(name = "PlanOrderResponse", description = "现货计划委托响应")
@Data
public class PlanOrderResponse {

    @Schema(description = "计划单ID", example = "20001")
    private Long planOrderId;

    @Schema(description = "计划单状态", example = "PENDING")
    private String status;

    @Schema(description = "触发后订单ID", example = "10001")
    private Long triggeredOrderId;

    public static PlanOrderResponse ok(Long planOrderId, String status, Long triggeredOrderId) {
        PlanOrderResponse response = new PlanOrderResponse();
        response.planOrderId = planOrderId;
        response.status = status;
        response.triggeredOrderId = triggeredOrderId;
        return response;
    }
}
