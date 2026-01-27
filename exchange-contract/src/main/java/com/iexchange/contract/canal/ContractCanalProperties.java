package com.iexchange.contract.canal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Canal 配置（合约订单同步）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "canal")
public class ContractCanalProperties {

    /**
     * 是否启用 Canal 同步。
     */
    private boolean enabled = true;

    /**
     * Canal Server 地址。
     */
    private String host = "127.0.0.1";

    /**
     * Canal Server 端口。
     */
    private int port = 11111;

    /**
     * Canal destination。
     */
    private String destination = "example";

    /**
     * Canal 用户名。
     */
    private String username = "";

    /**
     * Canal 密码。
     */
    private String password = "";

    /**
     * 拉取批次大小。
     */
    private int batchSize = 500;

    /**
     * 过滤规则（库.表）。
     */
    private String filter = "iexchange.contract_order";
}
