package com.iexchange.spot;

import org.dromara.easyes.starter.register.EsMapperScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * spot 服务启动类（骨架）。
 */
@SpringBootApplication
@MapperScan("com.iexchange.spot.mapper")
@EsMapperScan("com.iexchange.spot.es")
@EnableScheduling
public class SpotApplication {

    public static void main(String[] args) {
        // 启动当前服务
        SpringApplication.run(SpotApplication.class, args);
    }
}
