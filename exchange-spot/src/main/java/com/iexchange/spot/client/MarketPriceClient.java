package com.iexchange.spot.client;

import com.iexchange.common.response.R;
import com.iexchange.spot.dto.MarketTickerResult;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 行情价格查询客户端。
 */
@Slf4j
@Service
public class MarketPriceClient {
    private final RestTemplate restTemplate;
    private final String marketServiceUrl;

    public MarketPriceClient(RestTemplateBuilder builder,
                             @Value("${spot.plan.market-service-url:http://127.0.0.1:18085}") String marketServiceUrl) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(2))
            .build();
        this.marketServiceUrl = marketServiceUrl;
    }

    /**
     * 查询最新成交价。
     */
    public BigDecimal getLastPrice(String symbol) {
        try {
            String url = marketServiceUrl + "/api/market/ticker?symbol=" + symbol;
            ResponseEntity<R<MarketTickerResult>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<MarketTickerResult>>() {
                });
            R<MarketTickerResult> result = response.getBody();
            if (result == null || result.getCode() != R.SUCCESS_CODE || result.getData() == null) {
                return null;
            }
            return result.getData().getLastPrice();
        } catch (Exception ex) {
            log.warn("获取行情失败，symbol={}", symbol, ex);
            return null;
        }
    }
}
