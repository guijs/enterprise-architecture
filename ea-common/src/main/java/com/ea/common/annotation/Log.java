package com.ea.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 模块名称 */
    String title() default "";

    /** 业务类型：0 其它，1 新增，2 修改，3 删除，4 查询，5 导出，6 导入 */
    int businessType() default 0;

    /** 是否保存请求参数 */
    boolean saveRequestData() default true;

    /** 是否保存响应数据 */
    boolean saveResponseData() default false;
}
