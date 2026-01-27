package com.iexchange.spot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 成交事件对象。
 *
 * 字段说明：
 * - symbol 交易对
 * - buyOrderId 买单ID
 * - sellOrderId 卖单ID
 * - price 成交价格
 * - quantity 成交数量
 * - takerSide 主动方方向
 * - tradeTime 成交时间
 */
@Data
public class SpotTradeEvent {

    private String symbol;
    private Long buyOrderId;
    private Long sellOrderId;
    private BigDecimal price;
    private BigDecimal quantity;
    private String takerSide;
    private LocalDateTime tradeTime;


}
