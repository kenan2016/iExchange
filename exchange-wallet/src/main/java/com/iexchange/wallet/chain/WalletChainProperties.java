package com.iexchange.wallet.chain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 链上充提配置（本地 EVM 演示）。
 */
@Data
@ConfigurationProperties(prefix = "chain")
public class WalletChainProperties {

    /**
     * 是否启用链上功能。
     */
    private boolean enabled = false;

    /**
     * 链名称标识（用于地址与扫描记录分组）。
     */
    private String name = "local";

    /**
     * RPC 地址。
     */
    private String rpcUrl;

    /**
     * 链 ID（本地链通常为 1337/31337）。
     */
    private long chainId = 1337;

    /**
     * 需要确认的区块数。
     */
    private int confirmations = 1;

    private Token token = new Token();
    private HotWallet hotWallet = new HotWallet();
    private Scan scan = new Scan();
    private Sweep sweep = new Sweep();

    @Data
    public static class Token {
        /**
         * ERC20 合约地址。
         */
        private String address;
        /**
         * 资产符号（入账资产名）。
         */
        private String symbol = "IEX";
        /**
         * 小数精度。
         */
        private int decimals = 18;
    }

    @Data
    public static class HotWallet {
        /**
         * 热钱包私钥（留空则自动生成并写入数据库）。
         */
        private String privateKey;
        /**
         * 热钱包地址（留空则由私钥计算）。
         */
        private String address;
    }

    @Data
    public static class Scan {
        /**
         * 扫描间隔（毫秒）。
         */
        private long intervalMs = 5000;
        /**
         * 初始扫描区块（首次启动用）。
         */
        private long startBlock = 0;
    }

    @Data
    public static class Sweep {
        /**
         * 是否开启归集。
         */
        private boolean enabled = false;
        /**
         * 归集任务间隔（毫秒）。
         */
        private long intervalMs = 15000;
        /**
         * 最小归集数量（小于等于则跳过）。
         */
        private java.math.BigDecimal minAmount = java.math.BigDecimal.ZERO;
    }
}
