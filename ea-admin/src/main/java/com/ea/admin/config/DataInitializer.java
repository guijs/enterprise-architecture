package com.ea.admin.config;

import com.ea.system.domain.entity.SysUser;
import com.ea.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化默认管理员账号
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (sysUserService.getByUsername("admin") != null) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("系统管理员");
        admin.setEmail("admin@example.com");
        admin.setPhone("13800000000");
        admin.setGender(1);
        admin.setStatus(0);
        admin.setRemark("默认管理员账号，请及时修改密码");
        sysUserService.save(admin);
        log.info("已初始化默认管理员账号: admin / admin123");
    }
}
