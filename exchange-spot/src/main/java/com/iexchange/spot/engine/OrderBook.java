package com.iexchange.spot.engine;

import com.iexchange.spot.entity.SpotOrderEntity;
import com.iexchange.spot.enums.SpotOrderSide;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简化订单簿。
 *
 * 说明：
 * - 通过两个优先队列维护买盘/卖盘
 * - 队列排序规则：价格优先，时间优先
 * - 使用索引表加速撤单
 */
public class OrderBook {

    /**
     * 买盘队列（价格优先，时间优先）。
     */
    private final PriorityQueue<OrderBookEntry> buyQueue;

    /**
     * 卖盘队列（价格优先，时间优先）。
     */
    private final PriorityQueue<OrderBookEntry> sellQueue;

    /**
     * 订单索引，方便撤单。
     */
    private final Map<Long, OrderBookEntry> orderIndex;

    /**
     * 自增序列，用于时间优先排序。
     */
    private final AtomicLong sequence;

    public OrderBook() {
        // 买盘：价格高优先，价格相同时按入队序列先后
        this.buyQueue = new PriorityQueue<>((left, right) -> {
            int priceCompare = safePrice(right.getOrder()).compareTo(safePrice(left.getOrder()));
            if (priceCompare != 0) {
                return priceCompare;
            }
            return Long.compare(left.getSequence(), right.getSequence());
        });
        // 卖盘：价格低优先，价格相同时按入队序列先后
        this.sellQueue = new PriorityQueue<>((left, right) -> {
            int priceCompare = safePrice(left.getOrder()).compareTo(safePrice(right.getOrder()));
            if (priceCompare != 0) {
                return priceCompare;
            }
            return Long.compare(left.getSequence(), right.getSequence());
        });
        this.orderIndex = new HashMap<>();
        this.sequence = new AtomicLong(0);
    }

    /**
     * 添加订单到订单簿。
     *
     * 规则：
     * - 买单进入买盘队列（价格优先、时间优先）
     * - 卖单进入卖盘队列（价格优先、时间优先）
     * - 同时写入索引表，便于撤单快速定位
     */
    public OrderBookEntry add(SpotOrderEntity order, BigDecimal remainingQuantity) {
        OrderBookEntry entry = new OrderBookEntry(order, remainingQuantity, sequence.incrementAndGet(), false);
        if (SpotOrderSide.BUY.getCode().equals(order.getSide())) {
            buyQueue.add(entry);
        } else {
            sellQueue.add(entry);
        }
        orderIndex.put(order.getId(), entry);
        return entry;
    }

    /**
     * 获取当前买盘最优价订单（会清理已失效条目）。
     *
     * 通过 lazy-clean 的方式清理失效条目，避免每次撮合都全量遍历。
     */
    public OrderBookEntry peekBestBuy() {
        cleanQueue(buyQueue);
        return buyQueue.peek();
    }

    /**
     * 获取当前卖盘最优价订单（会清理已失效条目）。
     *
     * 通过 lazy-clean 的方式清理失效条目，避免每次撮合都全量遍历。
     */
    public OrderBookEntry peekBestSell() {
        cleanQueue(sellQueue);
        return sellQueue.peek();
    }

    /**
     * 撤销订单。
     *
     * 逻辑：
     * - 标记条目为已撤单
     * - 从买盘/卖盘队列中移除
     * - 从索引移除，避免后续再次访问
     */
    public boolean cancel(Long orderId) {
        OrderBookEntry entry = orderIndex.get(orderId);
        if (entry == null) {
            return false;
        }
        entry.setCanceled(true);
        buyQueue.remove(entry);
        sellQueue.remove(entry);
        orderIndex.remove(orderId);
        return true;
    }

    /**
     * 标记条目完成并移出队列。
     *
     * 通常用于：
     * - 全部成交
     * - 撮合结束后的清理
     */
    public void finishEntry(OrderBookEntry entry) {
        if (entry == null) {
            return;
        }
        entry.setCanceled(true);
        buyQueue.remove(entry);
        sellQueue.remove(entry);
        orderIndex.remove(entry.getOrderId());
    }

    /**
     * 清理队列顶部的失效条目，保证 peek 得到的是可成交的订单。
     *
     * 注意：这里只清理队头失效数据，保持 O(logN) 的处理成本。
     */
    private void cleanQueue(PriorityQueue<OrderBookEntry> queue) {
        while (!queue.isEmpty()) {
            OrderBookEntry top = queue.peek();
            if (top.isFinished()) {
                queue.poll();
                orderIndex.remove(top.getOrderId());
            } else {
                break;
            }
        }
    }

    /**
     * 空价格保护，避免比较时 NPE。
     */
    private BigDecimal safePrice(SpotOrderEntity order) {
        return order.getPrice() == null ? BigDecimal.ZERO : order.getPrice();
    }
}
