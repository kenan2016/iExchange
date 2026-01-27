package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 链上提币响应。
 */
@Schema(name = "WalletChainWithdrawResponse", description = "链上提币响应")
@Data
public class WalletChainWithdrawResponse {

    @Schema(description = "提币请求ID", example = "withdraw-1001")
    private String requestId;

    @Schema(description = "链上交易哈希", example = "0xabc123...")
    private String txHash;

    @Schema(description = "状态", example = "SUBMITTED")
    private String status;
}
