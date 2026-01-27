package com.iexchange.spot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 现货成交实体。
 *
 * 字段说明：
 * - id 主键
 * - symbol 交易对
 * - buyOrderId 买单ID
 * - sellOrderId 卖单ID
 * - price 成交价格
 * - quantity 成交数量
 * - takerSide 主动方方向
 * - createdAt 成交时间
 */
@TableName("spot_trade")
@Data
public class SpotTradeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String symbol;

    @TableField("buy_order_id")
    private Long buyOrderId;

    @TableField("sell_order_id")
    private Long sellOrderId;

    private BigDecimal price;

    private BigDecimal quantity;

    @TableField("taker_side")
    private String takerSide;

    @TableField("created_at")
    private LocalDateTime createdAt;


}
