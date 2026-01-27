package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OTC 订单动作请求。
 */
@Schema(name = "OtcOrderActionRequest", description = "OTC 订单动作请求")
@Data
public class OtcOrderActionRequest {

    @Schema(description = "订单号", example = "otc-10001")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
}
