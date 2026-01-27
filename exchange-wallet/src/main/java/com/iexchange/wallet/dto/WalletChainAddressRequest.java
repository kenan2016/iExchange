package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 链上充值地址申请请求。
 */
@Schema(name = "WalletChainAddressRequest", description = "链上充值地址申请请求")
@Data
public class WalletChainAddressRequest {

    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "链名称标识（可选，默认取配置）", example = "local")
    private String chainName;
}
