package com.iexchange.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 链上提币记录实体。
 */
@TableName("wallet_chain_withdraw")
@Data
public class WalletChainWithdrawEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String asset;

    @TableField("chain_name")
    private String chainName;

    private BigDecimal amount;

    @TableField("to_address")
    private String toAddress;

    @TableField("request_id")
    private String requestId;

    @TableField("tx_hash")
    private String txHash;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
