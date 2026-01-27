package com.iexchange.spot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 计划委托实体。
 *
 * 字段说明：
 * - id 主键
 * - userId 用户ID
 * - symbol 交易对
 * - side 方向：BUY/SELL
 * - type 类型：LIMIT/MARKET
 * - triggerPrice 触发价格
 * - orderPrice 触发后委托价
 * - quantity 委托数量
 * - status 计划单状态
 * - triggeredOrderId 触发后订单ID
 * - triggeredAt 触发时间
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@TableName("spot_plan_order")
@Data
public class SpotPlanOrderEntity {

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

    @TableField("trigger_price")
    private BigDecimal triggerPrice;

    @TableField("order_price")
    private BigDecimal orderPrice;

    private BigDecimal quantity;

    private String status;

    @TableField("triggered_order_id")
    private Long triggeredOrderId;

    @TableField("triggered_at")
    private LocalDateTime triggeredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
