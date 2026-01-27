package com.iexchange.market.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexchange.market.dto.SpotTradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 现货成交事件消费者。
 */
@Slf4j
@Component
public class SpotTradeConsumer {

    private final ObjectMapper objectMapper;
    private final MarketTradeDisruptor tradeDisruptor;

    public SpotTradeConsumer(ObjectMapper objectMapper,
                             MarketTradeDisruptor tradeDisruptor) {
        this.objectMapper = objectMapper;
        this.tradeDisruptor = tradeDisruptor;
    }

    /**
     * 监听成交事件主题，将消息投递到行情 Disruptor。
     */
    @KafkaListener(topics = "${market.trade.topic:spot.trade}")
    public void onMessage(String message) {
        try {
            SpotTradeEvent event = objectMapper.readValue(message, SpotTradeEvent.class);
            tradeDisruptor.publish(event);
        } catch (Exception ex) {
            log.warn("成交事件处理失败", ex);
        }
    }
}
