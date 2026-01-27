package com.iexchange.wallet.otc;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * OTC 超时任务。
 */
@Slf4j
@Component
public class OtcOrderScheduler {

    private final WalletOtcProperties properties;
    private final OtcOrderService orderService;

    public OtcOrderScheduler(WalletOtcProperties properties, OtcOrderService orderService) {
        this.properties = properties;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${otc.scan-interval-ms:5000}")
    public void autoCancelExpiredOrders() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(properties.getPayTimeoutMinutes());
            int count = orderService.autoCancelExpired(deadline);
            if (count > 0) {
                log.info("OTC 自动取消完成，count={}", count);
            }
        } catch (Exception ex) {
            log.warn("OTC 自动取消失败", ex);
        }
    }

    @Scheduled(fixedDelayString = "${otc.scan-interval-ms:5000}")
    public void autoAppealExpiredOrders() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(properties.getReleaseTimeoutMinutes());
            int count = orderService.autoAppealExpired(deadline);
            if (count > 0) {
                log.info("OTC 自动申诉完成，count={}", count);
            }
        } catch (Exception ex) {
            log.warn("OTC 自动申诉失败", ex);
        }
    }
}
