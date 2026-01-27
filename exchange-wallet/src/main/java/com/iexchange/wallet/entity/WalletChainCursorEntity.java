package com.iexchange.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 链上扫描游标实体。
 */
@TableName("wallet_chain_cursor")
@Data
public class WalletChainCursorEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("chain_name")
    private String chainName;

    @TableField("token_address")
    private String tokenAddress;

    @TableField("last_block")
    private Long lastBlock;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
