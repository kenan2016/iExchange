package com.iexchange.wallet.otc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OTC 配置（演示版）。
 */
@Data
@ConfigurationProperties(prefix = "otc")
public class WalletOtcProperties {

    /**
     * 是否启用 OTC 任务。
     */
    private boolean enabled = true;

    /**
     * 扫描间隔（毫秒）。
     */
    private long scanIntervalMs = 5000;

    /**
     * 买方付款超时时间（分钟）。
     */
    private long payTimeoutMinutes = 15;

    /**
     * 卖方放币超时时间（分钟）。
     */
    private long releaseTimeoutMinutes = 30;
}
