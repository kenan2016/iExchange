package com.iexchange.spot.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexchange.spot.dto.SpotTradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 成交事件生产者。
 */
@Slf4j
@Component
public class SpotTradeEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String tradeTopic;

    public SpotTradeEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${spot.trade.topic:spot.trade}") String tradeTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.tradeTopic = tradeTopic;
    }

    /**
     * 发送成交事件（，失败仅记录日志）。
     */
    public void sendTradeEvent(SpotTradeEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(tradeTopic, event.getSymbol(), payload);
        } catch (JsonProcessingException ex) {
            log.warn("成交事件序列化失败", ex);
        } catch (Exception ex) {
            log.warn("成交事件发送失败", ex);
        }
    }
}
