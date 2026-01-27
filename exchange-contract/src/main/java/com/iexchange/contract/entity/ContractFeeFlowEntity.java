package com.iexchange.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 合约手续费流水实体。
 *
 * 字段说明：
 * - id 主键
 * - orderId 订单ID
 * - userId 用户ID
 * - symbol 交易对
 * - action 开平动作
 * - side 方向
 * - feeRate 手续费费率
 * - feeAmount 手续费金额
 * - createdAt 创建时间
 */
@TableName("contract_fee_flow")
@Data
public class ContractFeeFlowEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("user_id")
    private Long userId;

    private String symbol;

    private String action;

    private String side;

    @TableField("fee_rate")
    private BigDecimal feeRate;

    @TableField("fee_amount")
    private BigDecimal feeAmount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
