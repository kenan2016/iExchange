package com.iexchange.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 成交事件对象。
 */
@Data
public class SpotTradeEvent {

    /**
     * 交易对。
     */
    private String symbol;

    /**
     * 买单 ID。
     */
    private Long buyOrderId;

    /**
     * 卖单 ID。
     */
    private Long sellOrderId;

    /**
     * 成交价格。
     */
    private BigDecimal price;

    /**
     * 成交数量。
     */
    private BigDecimal quantity;

    /**
     * 主动方方向（BUY/SELL）。
     */
    private String takerSide;

    /**
     * 成交时间。
     */
    private LocalDateTime tradeTime;


}
