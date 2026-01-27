package com.iexchange.spot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 现货手续费流水实体。
 *
 * 字段说明：
 * - id 主键
 * - tradeId 成交ID
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向
 * - feeAsset 手续费资产
 * - feeRate 手续费费率
 * - feeAmount 手续费金额
 * - createdAt 创建时间
 */
@TableName("spot_fee_flow")
@Data
public class SpotFeeFlowEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("trade_id")
    private Long tradeId;

    @TableField("user_id")
    private Long userId;

    private String symbol;

    private String side;

    @TableField("fee_asset")
    private String feeAsset;

    @TableField("fee_rate")
    private BigDecimal feeRate;

    @TableField("fee_amount")
    private BigDecimal feeAmount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
