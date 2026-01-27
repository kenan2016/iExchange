package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 钱包操作响应对象。
 *
 * 字段说明：
 * - userId 用户ID
 * - asset 资产类型
 * - availableBalance 可用余额
 * - frozenBalance 冻结余额
 * - totalBalance 总余额
 */
@Schema(name = "WalletResponse", description = "钱包操作响应")
@Data
public class WalletResponse {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "资产类型", example = "USDT")
    private String asset;

    @Schema(description = "可用余额", example = "1000")
    private BigDecimal availableBalance;

    @Schema(description = "冻结余额", example = "0")
    private BigDecimal frozenBalance;

    @Schema(description = "总余额", example = "1000")
    private BigDecimal totalBalance;

    public static WalletResponse ok(Long userId, String asset, BigDecimal available, BigDecimal frozen, BigDecimal total) {
        WalletResponse response = new WalletResponse();
        response.userId = userId;
        response.asset = asset;
        response.availableBalance = available;
        response.frozenBalance = frozen;
        response.totalBalance = total;
        return response;
    }
}
