package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合约撤单请求。
 *
 * 字段说明：
 * - userId 用户ID
 * - orderId 订单ID（仅撤销本人未成交订单）
 */
@Schema(name = "ContractOrderCancelRequest", description = "合约撤单请求")
@Data
public class ContractOrderCancelRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID（仅撤销本人未成交订单）", example = "30001")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
