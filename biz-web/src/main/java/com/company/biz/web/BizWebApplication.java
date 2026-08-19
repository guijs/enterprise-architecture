package com.company.biz.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BizWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizWebApplication.class, args);
    }
}
