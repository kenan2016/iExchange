package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 资金费率响应。
 *
 * 字段说明：
 * - symbol 交易对
 * - rate 资金费率（正数=多头支付，负数=多头收取）
 * - nextSettleTime 下次结算时间（Unix 秒）
 */
@Schema(name = "FundingRateResponse", description = "资金费率响应")
@Data
public class FundingRateResponse {

    @Schema(description = "交易对", example = "BTCUSDT-PERP")
    private String symbol;

    @Schema(description = "资金费率（正数=多头支付，负数=多头收取）", example = "0.0001")
    private BigDecimal rate;

    @Schema(description = "下次结算时间（Unix 秒）", example = "1700000000")
    private Long nextSettleTime;

    public static FundingRateResponse ok(String symbol, BigDecimal rate, Long nextSettleTime) {
        FundingRateResponse response = new FundingRateResponse();
        response.symbol = symbol;
        response.rate = rate;
        response.nextSettleTime = nextSettleTime;
        return response;
    }
}
