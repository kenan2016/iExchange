package com.iexchange.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资金费率结算实体。
 *
 * 字段说明：
 * - id 主键
 * - positionId 持仓ID
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向
 * - rate 资金费率
 * - markPrice 标记价格
 * - fundingAmount 资金费用
 * - settlementTime 结算时间（秒）
 * - createdAt 创建时间
 */
@TableName("contract_funding_settlement")
@Data
public class ContractFundingSettlementEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("position_id")
    private Long positionId;

    @TableField("user_id")
    private Long userId;

    private String symbol;

    private String side;

    private BigDecimal rate;

    @TableField("mark_price")
    private BigDecimal markPrice;

    @TableField("funding_amount")
    private BigDecimal fundingAmount;

    @TableField("settlement_time")
    private Long settlementTime;

    @TableField("created_at")
    private LocalDateTime createdAt;


}
