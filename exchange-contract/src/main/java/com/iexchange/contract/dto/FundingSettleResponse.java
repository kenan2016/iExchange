package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 资金费率结算响应。
 *
 * 字段说明：
 * - symbol 交易对
 * - rate 资金费率（正数=多头支付，负数=多头收取）
 * - positionCount 结算持仓数量
 * - totalAmount 资金费合计（可能为负）
 */
@Schema(name = "FundingSettleResponse", description = "资金费率结算响应")
@Data
public class FundingSettleResponse {

    @Schema(description = "交易对", example = "BTCUSDT-PERP")
    private String symbol;

    @Schema(description = "资金费率（正数=多头支付，负数=多头收取）", example = "0.0001")
    private BigDecimal rate;

    @Schema(description = "结算持仓数量", example = "10")
    private Integer positionCount;

    @Schema(description = "资金费合计（可能为负）", example = "12.5")
    private BigDecimal totalAmount;

    public static FundingSettleResponse ok(String symbol, BigDecimal rate, Integer positionCount, BigDecimal totalAmount) {
        FundingSettleResponse response = new FundingSettleResponse();
        response.symbol = symbol;
        response.rate = rate;
        response.positionCount = positionCount;
        response.totalAmount = totalAmount;
        return response;
    }
}
