package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 合约保证金账户请求。
 *
 * 字段说明：
 * - userId 用户ID
 * - amount 充值金额（转入合约保证金账户）
 */
@Schema(name = "ContractAccountRequest", description = "合约账户入金请求")
@Data
public class ContractAccountRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 充值金额。
     */
    @Schema(description = "充值金额（转入合约保证金账户）", example = "1000")
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00000001", message = "金额必须大于0")
    private BigDecimal amount;
}
