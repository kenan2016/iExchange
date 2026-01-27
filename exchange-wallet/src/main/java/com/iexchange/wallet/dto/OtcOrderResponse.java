package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * OTC 订单响应。
 */
@Schema(name = "OtcOrderResponse", description = "OTC 订单响应")
@Data
public class OtcOrderResponse {

    @Schema(description = "订单号", example = "otc-10001")
    private String orderNo;

    @Schema(description = "买方用户ID", example = "1")
    private Long buyerId;

    @Schema(description = "卖方用户ID", example = "2")
    private Long sellerId;

    @Schema(description = "资产类型", example = "USDT")
    private String asset;

    @Schema(description = "数量", example = "100")
    private BigDecimal amount;

    @Schema(description = "订单状态", example = "WAIT_PAY")
    private String status;

    @Schema(description = "付款时间")
    private LocalDateTime paidAt;

    @Schema(description = "放币时间")
    private LocalDateTime releasedAt;

    @Schema(description = "取消时间")
    private LocalDateTime canceledAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
