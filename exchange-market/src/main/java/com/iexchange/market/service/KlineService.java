package com.iexchange.market.service;

import com.iexchange.market.dto.SpotTradeEvent;
import com.iexchange.market.document.KlineDocument;
import java.util.List;

/**
 * K 线服务。
 */
public interface KlineService {

    /**
     * 接收成交事件并更新 K 线数据（内存桶 + 持久化 + 缓存）。
     */
    void onTrade(SpotTradeEvent event);

    /**
     * 查询指定交易对的 K 线列表。
     *
     * @param symbol   交易对
     * @param interval 周期（如 1m/5m/15m/30m/1h）
     * @param limit    返回条数
     */
    List<KlineDocument> query(String symbol, String interval, int limit);
}
