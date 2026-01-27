package com.iexchange.spot.engine;

import com.iexchange.spot.entity.SpotOrderEntity;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 订单簿条目。
 */
@Data
@AllArgsConstructor
public class OrderBookEntry {

    /**
     * 对应订单。
     */
    private final SpotOrderEntity order;

    /**
     * 剩余待成交数量。
     */
    private BigDecimal remainingQuantity;

    /**
     * 入队序列号（保证时间优先）。
     */
    private final long sequence;

    /**
     * 是否被撤销。
     */
    private boolean canceled;

    /**
     * 便捷获取订单 ID，用于索引与移除。
     */
    public Long getOrderId() {
        return order.getId();
    }

    /**
     * 扣减剩余数量（成交后更新剩余量）。
     */
    public void reduce(BigDecimal quantity) {
        this.remainingQuantity = this.remainingQuantity.subtract(quantity);
    }

    /**
     * 判断条目是否失效（被撤单或已成交完毕）。
     */
    public boolean isFinished() {
        return canceled || remainingQuantity.compareTo(BigDecimal.ZERO) <= 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OrderBookEntry other = (OrderBookEntry) obj;
        return getOrderId() != null && getOrderId().equals(other.getOrderId());
    }

    @Override
    public int hashCode() {
        return getOrderId() == null ? 0 : getOrderId().hashCode();
    }
}
