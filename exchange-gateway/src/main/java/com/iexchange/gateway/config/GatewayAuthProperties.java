package com.iexchange.gateway.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关鉴权配置。
 */
@Component
@ConfigurationProperties(prefix = "gateway.auth")
@Data
public class GatewayAuthProperties {

    /**
     * 是否启用网关鉴权。
     */
    private boolean enabled = true;

    /**
     * 不需要鉴权的路径（支持 Ant 风格）。
     */
    private List<String> ignorePaths = new ArrayList<>();
}
