package com.iexchange.wallet;

import com.iexchange.wallet.chain.WalletChainProperties;
import com.iexchange.wallet.otc.WalletOtcProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * wallet 服务启动类（骨架）。
 */
@SpringBootApplication
@MapperScan("com.iexchange.wallet.mapper") // 扫描 Mapper 接口
@EnableScheduling
@EnableConfigurationProperties({WalletChainProperties.class, WalletOtcProperties.class})
public class WalletApplication {

    public static void main(String[] args) {
        // 启动当前服务
        SpringApplication.run(WalletApplication.class, args);
    }
}
