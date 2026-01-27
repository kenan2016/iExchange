package com.iexchange.contract.es;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.IndexId;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.rely.FieldType;

/**
 * 合约订单索引文档。
 */
@IndexName("contract_order")
@Data
public class ContractOrderDocument {

    @IndexId
    private Long id;

    @IndexField(fieldType = FieldType.LONG)
    private Long userId;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String symbol;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String action;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String side;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String type;

    @IndexField(fieldType = FieldType.DOUBLE)
    private BigDecimal price;

    @IndexField(fieldType = FieldType.DOUBLE)
    private BigDecimal quantity;

    @IndexField(fieldType = FieldType.INTEGER)
    private Integer leverage;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String marginMode;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String status;

    @IndexField(fieldType = FieldType.DOUBLE)
    private BigDecimal filledPrice;

    @IndexField(fieldType = FieldType.DATE)
    private LocalDateTime createdAt;

    @IndexField(fieldType = FieldType.DATE)
    private LocalDateTime updatedAt;
}
