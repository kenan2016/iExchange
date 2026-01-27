package com.iexchange.api.wallet;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 钱包账户 DTO（Dubbo 传输对象，示例）。
 */
@Data
public class WalletAccountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 资产符号（例如 USDT）。
     */
    private String asset;

    /**
     * 可用余额。
     */
    private BigDecimal availableBalance;

    /**
     * 冻结余额。
     */
    private BigDecimal frozenBalance;

    /**
     * 总余额（可用 + 冻结）。
     */
    private BigDecimal totalBalance;
}
