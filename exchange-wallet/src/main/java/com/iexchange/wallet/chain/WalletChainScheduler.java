package com.iexchange.wallet.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 链上扫描任务。
 */
@Slf4j
@Component
public class WalletChainScheduler {

    private final WalletChainProperties properties;
    private final WalletChainService chainService;

    public WalletChainScheduler(WalletChainProperties properties, WalletChainService chainService) {
        this.properties = properties;
        this.chainService = chainService;
    }

    @Scheduled(fixedDelayString = "${chain.scan.interval-ms:5000}")
    public void scanDeposits() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            int count = chainService.syncDeposits();
            if (count > 0) {
                log.info("链上充值入账完成，count={}", count);
            }
        } catch (Exception ex) {
            log.warn("链上充值扫描失败", ex);
        }
    }

    @Scheduled(fixedDelayString = "${chain.scan.interval-ms:5000}")
    public void confirmWithdraws() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            int count = chainService.confirmWithdraws();
            if (count > 0) {
                log.info("链上提币确认完成，count={}", count);
            }
        } catch (Exception ex) {
            log.warn("链上提币确认失败", ex);
        }
    }

    @Scheduled(fixedDelayString = "${chain.sweep.interval-ms:15000}")
    public void sweepDeposits() {
        if (!properties.isEnabled() || !properties.getSweep().isEnabled()) {
            return;
        }
        try {
            int count = chainService.sweepDeposits();
            if (count > 0) {
                log.info("链上归集完成，count={}", count);
            }
        } catch (Exception ex) {
            log.warn("链上归集失败", ex);
        }
    }
}
