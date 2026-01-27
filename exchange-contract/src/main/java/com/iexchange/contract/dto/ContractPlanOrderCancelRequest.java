package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合约计划委托撤单请求。
 *
 * 字段说明：
 * - userId 用户ID
 * - planOrderId 计划单ID（仅撤销本人未触发计划单）
 */
@Schema(name = "ContractPlanOrderCancelRequest", description = "合约计划委托撤单请求")
@Data
public class ContractPlanOrderCancelRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 计划单ID。
     */
    @Schema(description = "计划单ID（仅撤销本人未触发计划单）", example = "40001")
    @NotNull(message = "计划单ID不能为空")
    private Long planOrderId;
}
