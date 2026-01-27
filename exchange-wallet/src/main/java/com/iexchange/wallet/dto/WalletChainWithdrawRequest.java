package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 链上提币请求。
 */
@Schema(name = "WalletChainWithdrawRequest", description = "链上提币请求")
@Data
public class WalletChainWithdrawRequest {

    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "资产符号", example = "IEX")
    @NotBlank(message = "资产不能为空")
    private String asset;

    @Schema(description = "提币数量", example = "10")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.00000001", message = "数量必须大于0")
    private BigDecimal amount;

    @Schema(description = "提币地址", example = "0x1234...abcd")
    @NotBlank(message = "提币地址不能为空")
    private String toAddress;

    @Schema(description = "幂等请求ID", example = "withdraw-1001")
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
