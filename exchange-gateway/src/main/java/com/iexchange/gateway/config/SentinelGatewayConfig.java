package com.iexchange.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel 网关限流规则配置。
 */
@Configuration
public class SentinelGatewayConfig {

    @PostConstruct
    public void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        // 对路由 "exchange-user" 设置 QPS 限流（每秒 5 个请求）
        rules.add(new GatewayFlowRule("exchange-user").setCount(5).setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
    }
}
