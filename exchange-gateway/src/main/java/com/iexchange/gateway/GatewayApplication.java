package com.iexchange.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * gateway 服务启动类（骨架）。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        // 启动当前服务
        SpringApplication.run(GatewayApplication.class, args);
    }
}
