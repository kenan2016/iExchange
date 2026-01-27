package com.iexchange.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Seata 极简演示订单实体。
 */
@Data
@TableName("demo_seata_order")
public class DemoSeataOrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String asset;

    private BigDecimal amount;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
