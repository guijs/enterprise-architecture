package com.ea.system.controller;

import com.ea.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 应用探活
 */
@Tag(name = "系统探活")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "应用健康检查")
    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        return R.ok(Map.of(
                "app", "enterprise-architecture",
                "status", "UP",
                "time", LocalDateTime.now().toString()
        ));
    }
}
