package com.iexchange.spot.engine;

import com.iexchange.spot.entity.SpotOrderEntity;
import lombok.Data;

/**
 * 撮合结果。
 *
 * 用于在 Disruptor 线程与业务线程之间传递处理结果。
 */
@Data
public class SpotMatchingResult {

    /**
     * 最终订单状态（撮合完成后的最新订单）。
     */
    private SpotOrderEntity order;

    /**
     * 是否撤单成功。
     */
    private boolean canceled;
}
