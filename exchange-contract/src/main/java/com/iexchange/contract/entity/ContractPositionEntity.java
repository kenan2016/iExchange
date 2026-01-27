package com.iexchange.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 合约持仓实体。
 *
 * 字段说明：
 * - id 主键
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向：LONG/SHORT
 * - marginMode 保证金模式
 * - leverage 杠杆倍数
 * - quantity 持仓数量
 * - entryPrice 开仓均价
 * - margin 占用保证金
 * - liquidationPrice 强平价
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@TableName("contract_position")
@Data
public class ContractPositionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String symbol;

    /**
     * LONG/SHORT。
     */
    private String side;

    /**
     * CROSS/ISOLATED。
     */
    @TableField("margin_mode")
    private String marginMode;

    private Integer leverage;

    private BigDecimal quantity;

    @TableField("entry_price")
    private BigDecimal entryPrice;

    private BigDecimal margin;

    @TableField("liquidation_price")
    private BigDecimal liquidationPrice;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
