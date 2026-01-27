package com.iexchange.spot.engine;

import com.iexchange.spot.entity.SpotOrderEntity;
import lombok.Data;

/**
 * 撮合结果。
 */
@Data
public class SpotMatchingResult {

    /**
     * 最终订单状态。
     */
    private SpotOrderEntity order;

    /**
     * 是否撤单。
     */
    private boolean canceled;
}
