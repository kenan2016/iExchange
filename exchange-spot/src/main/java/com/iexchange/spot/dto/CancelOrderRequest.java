package com.iexchange.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 撤单请求。
 *
 * 字段说明：
 * - userId 用户ID
 * - orderId 订单ID
 */
@Schema(name = "CancelOrderRequest", description = "现货撤单请求")
@Data
public class CancelOrderRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID", example = "10001")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
