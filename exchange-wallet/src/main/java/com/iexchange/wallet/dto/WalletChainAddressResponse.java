package com.iexchange.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 链上充值地址响应。
 */
@Schema(name = "WalletChainAddressResponse", description = "链上充值地址响应")
@Data
public class WalletChainAddressResponse {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "链名称标识", example = "local")
    private String chainName;

    @Schema(description = "链上地址", example = "0x1234...abcd")
    private String address;
}
