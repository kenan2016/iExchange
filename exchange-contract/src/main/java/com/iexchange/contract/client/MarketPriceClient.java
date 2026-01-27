package com.iexchange.contract.client;

import com.iexchange.common.response.R;
import com.iexchange.contract.dto.MarketTickerResult;
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
 * 合约行情价格查询客户端。
 */
@Slf4j
@Service
public class MarketPriceClient {

    private final RestTemplate restTemplate;
    private final String marketServiceUrl;

    public MarketPriceClient(RestTemplateBuilder builder,
                             @Value("${contract.market.service-url:http://127.0.0.1:18085}") String marketServiceUrl) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(2))
            .build();
        this.marketServiceUrl = marketServiceUrl;
    }

    /**
     * 查询合约标记价（简化为映射现货最新价）。
     *
     * 说明：
     * - 标记价通常用于避免被“瞬时成交价”操纵触发强平
     * - 这里为了演示，直接用现货行情做标记价
     */
    public BigDecimal getMarkPrice(String contractSymbol) {
        String spotSymbol = resolveSpotSymbol(contractSymbol);
        if (spotSymbol == null) {
            return null;
        }
        try {
            // 复用现货行情作为合约标记价（演示版）
            String url = marketServiceUrl + "/api/market/ticker?symbol=" + spotSymbol;
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
            log.warn("获取合约行情失败，symbol={}", contractSymbol, ex);
            return null;
        }
    }

    /**
     * 将合约交易对映射为现货交易对。
     */
    private String resolveSpotSymbol(String contractSymbol) {
        if (contractSymbol == null || contractSymbol.isBlank()) {
            return null;
        }
        String normalized = contractSymbol.trim().toUpperCase();
        if (normalized.endsWith("-PERP")) {
            // BTCUSDT-PERP -> BTC_USDT
            String base = normalized.substring(0, normalized.length() - 5);
            int index = base.lastIndexOf("USDT");
            if (index > 0) {
                return base.substring(0, index) + "_" + base.substring(index);
            }
        }
        return contractSymbol;
    }
}
