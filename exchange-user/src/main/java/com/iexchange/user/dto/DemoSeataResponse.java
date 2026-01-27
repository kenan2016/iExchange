package com.iexchange.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Seata 极简演示响应。
 */
@Schema(name = "DemoSeataResponse", description = "Seata 极简演示响应")
@Data
public class DemoSeataResponse {

    /**
     * 演示订单 ID。
     */
    @Schema(description = "演示订单ID", example = "1001")
    private Long orderId;

    /**
     * 钱包可用余额（演示用）。
     */
    @Schema(description = "可用余额", example = "1000")
    private BigDecimal availableBalance;

    /**
     * 钱包总余额（演示用）。
     */
    @Schema(description = "总余额", example = "1000")
    private BigDecimal totalBalance;
}
