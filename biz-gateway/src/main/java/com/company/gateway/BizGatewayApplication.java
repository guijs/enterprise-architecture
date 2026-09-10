package com.company.gateway;

import com.company.gateway.config.AuthWhitelistProperties;
import com.company.gateway.config.GatewayCorsProperties;
import com.company.gateway.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, AuthWhitelistProperties.class, GatewayCorsProperties.class})
public class BizGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizGatewayApplication.class, args);
    }
}
