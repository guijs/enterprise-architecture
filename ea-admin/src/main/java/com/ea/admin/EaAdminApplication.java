package com.ea.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业基础架构启动入口
 */
@SpringBootApplication(scanBasePackages = "com.ea")
@MapperScan("com.ea.**.mapper")
public class EaAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(EaAdminApplication.class, args);
    }
}
