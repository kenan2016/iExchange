package com.iexchange.spot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 现货订单实体。
 *
 * 字段说明：
 * - id 主键
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向：BUY/SELL
 * - type 类型：LIMIT/MARKET
 * - price 委托价格
 * - quantity 委托数量
 * - filledQuantity 已成交数量
 * - status 订单状态
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@TableName("spot_order")
@Data
public class SpotOrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String symbol;

    /**
     * BUY/SELL。
     */
    private String side;

    /**
     * LIMIT/MARKET。
     */
    private String type;

    private BigDecimal price;

    private BigDecimal quantity;

    @TableField("filled_quantity")
    private BigDecimal filledQuantity;

    /**
     * NEW/PARTIAL_FILLED/FILLED/CANCELED。
     */
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
