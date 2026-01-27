package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 合约保证金账户响应。
 *
 * 字段说明：
 * - userId 用户ID
 * - balance 保证金余额（可用保证金）
 */
@Schema(name = "ContractAccountResponse", description = "合约账户响应")
@Data
public class ContractAccountResponse {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "保证金余额（可用于开仓的可用保证金）", example = "1000")
    private BigDecimal balance;

    public static ContractAccountResponse ok(Long userId, BigDecimal balance) {
        ContractAccountResponse response = new ContractAccountResponse();
        response.userId = userId;
        response.balance = balance;
        return response;
    }
}
