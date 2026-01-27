package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 资金费率结算请求。
 *
 * 字段说明：
 * - symbol 交易对
 * - markPrice 标记价格（用于结算资金费）
 * - indexPrice 指数价格（参考价）
 */
@Schema(name = "FundingSettleRequest", description = "资金费率结算请求")
@Data
public class FundingSettleRequest {

    /**
     * 交易对。
     */
    @Schema(description = "交易对", example = "BTCUSDT-PERP")
    @NotBlank(message = "交易对不能为空")
    private String symbol;

    /**
     * 标记价格。
     */
    @Schema(description = "标记价格（用于结算资金费）", example = "30000")
    @NotNull(message = "标记价格不能为空")
    @DecimalMin(value = "0.00000001", message = "标记价格必须大于0")
    private BigDecimal markPrice;

    /**
     * 指数价格。
     */
    @Schema(description = "指数价格（参考价）", example = "29950")
    @NotNull(message = "指数价格不能为空")
    @DecimalMin(value = "0.00000001", message = "指数价格必须大于0")
    private BigDecimal indexPrice;
}
