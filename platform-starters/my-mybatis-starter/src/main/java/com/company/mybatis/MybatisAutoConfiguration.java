package com.company.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * MyBatis-Plus 自动装配。
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@Import(MybatisPlusConfig.class)
public class MybatisAutoConfiguration {
}
