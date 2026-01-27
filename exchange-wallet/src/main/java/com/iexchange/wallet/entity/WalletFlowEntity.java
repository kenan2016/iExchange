package com.iexchange.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 钱包流水实体。
 *
 * 字段说明：
 * - id 主键
 * - userId 用户ID
 * - asset 资产类型
 * - flowType 流水类型
 * - amount 变动金额
 * - balanceAfter 变动后余额
 * - businessId 业务请求ID（幂等）
 * - createdAt 创建时间
 */
@TableName("wallet_flow")
@Data
public class WalletFlowEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String asset;

    @TableField("flow_type")
    private String flowType;

    private BigDecimal amount;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("business_id")
    private String businessId;

    @TableField("created_at")
    private LocalDateTime createdAt;


}
