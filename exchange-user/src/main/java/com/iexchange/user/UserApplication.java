package com.iexchange.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * user 服务启动类（骨架）。
 */
@SpringBootApplication
@MapperScan("com.iexchange.user.mapper") // 扫描 Mapper 接口
public class UserApplication {

    public static void main(String[] args) {
        // 启动当前服务
        SpringApplication.run(UserApplication.class, args);
    }
}
