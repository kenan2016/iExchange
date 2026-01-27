package com.iexchange.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Seata 极简演示请求。
 */
@Schema(name = "DemoSeataRequest", description = "Seata 极简演示请求")
@Data
public class DemoSeataRequest {

    /**
     * 用户 ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 资产类型，例如 USDT。
     */
    @Schema(description = "资产类型", example = "USDT")
    @NotBlank(message = "资产不能为空")
    private String asset;

    /**
     * 入账金额。
     */
    @Schema(description = "入账金额", example = "10")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    /**
     * 备注（）。
     */
    @Schema(description = "备注", example = "seata-demo")
    private String remark;

    /**
     * 是否强制失败（用于演示回滚）。
     */
    @Schema(description = "是否强制失败", example = "false")
    private Boolean forceFail;
}
