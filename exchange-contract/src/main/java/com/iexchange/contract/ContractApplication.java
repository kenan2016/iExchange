package com.iexchange.contract;

import org.dromara.easyes.starter.register.EsMapperScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * contract 服务启动类（骨架）。
 */
@SpringBootApplication
@MapperScan("com.iexchange.contract.mapper")
@EsMapperScan("com.iexchange.contract.es")
@EnableScheduling
public class ContractApplication {

    public static void main(String[] args) {
        // 启动当前服务
        SpringApplication.run(ContractApplication.class, args);
    }
}
