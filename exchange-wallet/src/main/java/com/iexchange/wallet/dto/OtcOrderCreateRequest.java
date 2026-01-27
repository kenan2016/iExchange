package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * OTC 下单请求。
 */
@Schema(name = "OtcOrderCreateRequest", description = "OTC 下单请求")
@Data
public class OtcOrderCreateRequest {

    @Schema(description = "订单号", example = "otc-10001")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "买方用户ID", example = "1")
    @NotNull(message = "买方用户ID不能为空")
    private Long buyerId;

    @Schema(description = "卖方用户ID", example = "2")
    @NotNull(message = "卖方用户ID不能为空")
    private Long sellerId;

    @Schema(description = "资产类型", example = "USDT")
    @NotBlank(message = "资产不能为空")
    private String asset;

    @Schema(description = "数量", example = "100")
    @NotNull(message = "数量不能为空")
    private BigDecimal amount;
}
