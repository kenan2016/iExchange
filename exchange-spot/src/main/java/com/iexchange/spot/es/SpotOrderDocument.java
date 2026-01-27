package com.iexchange.spot.es;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.dromara.easyes.annotation.IndexId;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.rely.FieldType;

/**
 * 现货订单索引文档。
 */
@IndexName("spot_order")
@Data
public class SpotOrderDocument {

    @IndexId
    private Long id;

    @IndexField(fieldType = FieldType.LONG)
    private Long userId;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String symbol;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String side;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String type;

    @IndexField(fieldType = FieldType.DOUBLE)
    private BigDecimal price;

    @IndexField(fieldType = FieldType.DOUBLE)
    private BigDecimal quantity;

    @IndexField(fieldType = FieldType.DOUBLE)
    private BigDecimal filledQuantity;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String status;

    @IndexField(fieldType = FieldType.DATE)
    private LocalDateTime createdAt;

    @IndexField(fieldType = FieldType.DATE)
    private LocalDateTime updatedAt;
}
