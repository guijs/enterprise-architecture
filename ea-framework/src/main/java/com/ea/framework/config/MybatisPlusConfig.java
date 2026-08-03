package com.ea.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.ea.framework.security.LoginUser;
import com.ea.framework.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
                strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
                strictInsertFill(metaObject, "deleted", () -> 0, Integer.class);
                String username = currentUsername();
                strictInsertFill(metaObject, "createBy", () -> username, String.class);
                strictInsertFill(metaObject, "updateBy", () -> username, String.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
                strictUpdateFill(metaObject, "updateBy", this::currentUsername, String.class);
            }

            private String currentUsername() {
                LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
                return loginUser == null ? "system" : loginUser.getUsername();
            }
        };
    }
}
