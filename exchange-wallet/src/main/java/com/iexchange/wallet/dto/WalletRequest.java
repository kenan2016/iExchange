package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 钱包操作请求对象。
 *
 * 字段说明：
 * - userId 用户ID
 * - asset 资产类型
 * - amount 数量
 * - requestId 幂等请求ID
 */
@Schema(name = "WalletRequest", description = "钱包操作请求")
@Data
public class WalletRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 资产类型。
     */
    @Schema(description = "资产类型", example = "USDT")
    @NotBlank(message = "资产不能为空")
    private String asset;

    /**
     * 操作数量。
     */
    @Schema(description = "操作数量", example = "100")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.00000001", message = "数量必须大于0")
    private BigDecimal amount;

    /**
     * 幂等请求ID。
     */
    @Schema(description = "幂等请求ID", example = "deposit-1001")
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
