package com.iexchange.market.service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 深度档位。
 */
@Schema(name = "DepthLevel", description = "盘口深度档位")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepthLevel {

    /**
     * 价格。
     */
    @Schema(description = "价格", example = "30000")
    private BigDecimal price;

    /**
     * 数量。
     */
    @Schema(description = "数量", example = "0.5")
    private BigDecimal quantity;
}
