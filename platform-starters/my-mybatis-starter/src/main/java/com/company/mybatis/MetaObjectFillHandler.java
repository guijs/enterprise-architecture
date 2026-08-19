package com.company.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.company.security.UserContext;
import com.company.security.UserInfo;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 自动填充：创建/更新时间与操作人，操作人取自用户上下文。
 */
public class MetaObjectFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "createBy", this::currentUser, String.class);
        this.strictInsertFill(metaObject, "updateBy", this::currentUser, String.class);
        this.strictInsertFill(metaObject, "version", () -> 0, Integer.class);
        this.strictInsertFill(metaObject, "deleted", () -> 0, Integer.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateBy", this::currentUser, String.class);
    }

    private String currentUser() {
        return Optional.ofNullable(UserContext.get()).map(UserInfo::getUserId).orElse("system");
    }
}
