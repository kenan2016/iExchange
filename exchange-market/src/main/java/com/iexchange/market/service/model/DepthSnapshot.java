package com.iexchange.market.service.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 深度快照。
 */
@Data
public class DepthSnapshot {

    /**
     * 交易对。
     */
    private String symbol;

    /**
     * 买盘档位。
     */
    private List<DepthLevel> bids;

    /**
     * 卖盘档位。
     */
    private List<DepthLevel> asks;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;


}
