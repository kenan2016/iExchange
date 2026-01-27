package com.iexchange.contract.dto;

import com.iexchange.contract.entity.ContractPositionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 合约持仓响应。
 *
 * 字段说明：
 * - positionId 持仓ID
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向：LONG=看涨做多，SHORT=看跌做空
 * - marginMode 保证金模式：CROSS=全仓，ISOLATED=逐仓
 * - leverage 杠杆倍数
 * - quantity 持仓数量（合约数量）
 * - entryPrice 开仓均价（持仓平均成本价）
 * - margin 占用保证金（该仓位当前占用）
 * - liquidationPrice 强平价（标记价触达可能被强平）
 */
@Schema(name = "ContractPositionResponse", description = "合约持仓响应")
@Data
public class ContractPositionResponse {

    @Schema(description = "持仓ID", example = "50001")
    private Long positionId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "交易对", example = "BTCUSDT-PERP")
    private String symbol;

    @Schema(description = "方向：LONG=看涨做多，SHORT=看跌做空", example = "LONG")
    private String side;

    @Schema(description = "保证金模式：CROSS=全仓，ISOLATED=逐仓", example = "CROSS")
    private String marginMode;

    @Schema(description = "杠杆倍数", example = "10")
    private Integer leverage;

    @Schema(description = "持仓数量（合约数量）", example = "1")
    private BigDecimal quantity;

    @Schema(description = "开仓均价（持仓平均成本价）", example = "30000")
    private BigDecimal entryPrice;

    @Schema(description = "占用保证金（该仓位当前占用的保证金）", example = "100")
    private BigDecimal margin;

    @Schema(description = "强平价（标记价触达该价可能被强平）", example = "25000")
    private BigDecimal liquidationPrice;

    public static ContractPositionResponse ok(ContractPositionEntity position) {
        ContractPositionResponse response = new ContractPositionResponse();
        response.positionId = position.getId();
        response.userId = position.getUserId();
        response.symbol = position.getSymbol();
        response.side = position.getSide();
        response.marginMode = position.getMarginMode();
        response.leverage = position.getLeverage();
        response.quantity = position.getQuantity();
        response.entryPrice = position.getEntryPrice();
        response.margin = position.getMargin();
        response.liquidationPrice = position.getLiquidationPrice();
        return response;
    }
}
