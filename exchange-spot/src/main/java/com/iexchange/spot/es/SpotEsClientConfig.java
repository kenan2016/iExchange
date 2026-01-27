package com.iexchange.spot.es;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ES 客户端配置（演示版）。
 */
@Configuration
public class SpotEsClientConfig {

    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient(
        @Value("${es.host:http://127.0.0.1:9200}") String host) {
        return new RestHighLevelClient(RestClient.builder(HttpHost.create(host)));
    }
}
