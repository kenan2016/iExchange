package com.iexchange.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 合约订单请求。
 *
 * 字段说明：
 * - userId 用户ID
 * - symbol 交易对
 * - action 开平动作：OPEN=开仓，CLOSE=平仓
 * - side 方向：LONG=看涨做多，SHORT=看跌做空
 * - type 类型：LIMIT=限价，MARKET=市价
 * - quantity 委托数量（合约数量）
 * - price 执行价格（市价单为保护价）
 * - leverage 杠杆倍数（倍数越大占用保证金越少）
 * - marginMode 保证金模式：CROSS=全仓，ISOLATED=逐仓
 */
@Schema(name = "ContractOrderRequest", description = "合约下单请求")
@Data
public class ContractOrderRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 交易对。
     */
    @Schema(description = "交易对", example = "BTCUSDT-PERP")
    @NotBlank(message = "交易对不能为空")
    private String symbol;

    /**
     * 开平动作：OPEN/CLOSE。
     */
    @Schema(description = "开平动作：OPEN=开仓，CLOSE=平仓", example = "OPEN")
    @NotBlank(message = "开平动作不能为空")
    private String action;

    /**
     * 方向：LONG/SHORT。
     */
    @Schema(description = "方向：LONG=看涨做多，SHORT=看跌做空", example = "LONG")
    @NotBlank(message = "方向不能为空")
    private String side;

    /**
     * 类型：LIMIT/MARKET。
     */
    @Schema(description = "类型：LIMIT=限价挂单，MARKET=市价成交", example = "LIMIT")
    @NotBlank(message = "类型不能为空")
    private String type;

    /**
     * 委托数量。
     */
    @Schema(description = "委托数量（合约数量）", example = "1")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.00000001", message = "数量必须大于0")
    private BigDecimal quantity;

    /**
     * 执行价格（，市场单也需要传入）。
     */
    @Schema(description = "执行价格（市价单为保护价，用于限制极端滑点）", example = "30000")
    private BigDecimal price;

    /**
     * 杠杆倍数（开仓必填）。
     */
    @Schema(description = "杠杆倍数（开仓必填，倍数越大占用保证金越少）", example = "10")
    private Integer leverage;

    /**
     * 保证金模式（开仓必填）。
     */
    @Schema(description = "保证金模式：CROSS=全仓，ISOLATED=逐仓", example = "CROSS")
    private String marginMode;
}
