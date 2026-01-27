package com.iexchange.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 计划委托撤单请求。
 *
 * 字段说明：
 * - userId 用户ID
 * - planOrderId 计划单ID
 */
@Schema(name = "PlanOrderCancelRequest", description = "现货计划委托撤单请求")
@Data
public class PlanOrderCancelRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 计划单ID。
     */
    @Schema(description = "计划单ID", example = "20001")
    @NotNull(message = "计划单ID不能为空")
    private Long planOrderId;
}
