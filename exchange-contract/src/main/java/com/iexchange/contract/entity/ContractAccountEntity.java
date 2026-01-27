package com.iexchange.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 合约保证金账户实体。
 *
 * 字段说明：
 * - id 主键
 * - userId 用户ID
 * - balance 可用保证金余额
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@TableName("contract_account")
@Data
public class ContractAccountEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 可用保证金余额。
     */
    private BigDecimal balance;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
