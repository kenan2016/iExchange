package com.iexchange.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 合约订单实体。
 *
 * 字段说明：
 * - id 主键
 * - userId 用户ID
 * - symbol 交易对
 * - action 开平动作：OPEN/CLOSE
 * - side 方向：LONG/SHORT
 * - type 类型：LIMIT/MARKET
 * - price 委托价格
 * - quantity 委托数量
 * - leverage 杠杆倍数
 * - marginMode 保证金模式
 * - status 订单状态
 * - filledPrice 成交价格
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@TableName("contract_order")
@Data
public class ContractOrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String symbol;

    /**
     * OPEN/CLOSE。
     */
    private String action;

    /**
     * LONG/SHORT。
     */
    private String side;

    /**
     * LIMIT/MARKET。
     */
    private String type;

    private BigDecimal price;

    private BigDecimal quantity;

    private Integer leverage;

    @TableField("margin_mode")
    private String marginMode;

    private String status;

    @TableField("filled_price")
    private BigDecimal filledPrice;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
