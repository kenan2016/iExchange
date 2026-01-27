package com.iexchange.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 钱包账户实体。
 *
 * 字段说明：
 * - id 主键
 * - userId 用户ID
 * - asset 资产类型
 * - availableBalance 可用余额
 * - frozenBalance 冻结余额
 * - totalBalance 总余额
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@TableName("wallet_account")
@Data
public class WalletAccountEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String asset;

    @TableField("available_balance")
    private BigDecimal availableBalance;

    @TableField("frozen_balance")
    private BigDecimal frozenBalance;

    @TableField("total_balance")
    private BigDecimal totalBalance;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
