package com.iexchange.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 合约交易对实体。
 *
 * 字段说明：
 * - id 主键
 * - symbol 交易对
 * - baseAsset 基础资产
 * - quoteAsset 计价资产
 * - priceScale 价格精度
 * - quantityScale 数量精度
 * - maxLeverage 最大杠杆
 * - status 状态：1启用，0禁用
 * - createdAt 创建时间
 * - updatedAt 更新时间
 */
@TableName("contract_symbol")
@Data
public class ContractSymbolEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String symbol;

    @TableField("base_asset")
    private String baseAsset;

    @TableField("quote_asset")
    private String quoteAsset;

    @TableField("price_scale")
    private Integer priceScale;

    @TableField("quantity_scale")
    private Integer quantityScale;

    @TableField("max_leverage")
    private Integer maxLeverage;

    /**
     * 状态：1 启用，0 禁用。
     */
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
