package com.iexchange.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资金费率记录实体。
 *
 * 字段说明：
 * - id 主键
 * - symbol 交易对
 * - rate 资金费率
 * - markPrice 标记价格
 * - indexPrice 指数价格
 * - nextSettleTime 下次结算时间（秒）
 * - createdAt 创建时间
 */
@TableName("contract_funding_rate")
@Data
public class ContractFundingRateEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String symbol;

    private BigDecimal rate;

    @TableField("mark_price")
    private BigDecimal markPrice;

    @TableField("index_price")
    private BigDecimal indexPrice;

    @TableField("next_settle_time")
    private Long nextSettleTime;

    @TableField("created_at")
    private LocalDateTime createdAt;


}
