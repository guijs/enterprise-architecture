-- Enterprise Architecture 初始化脚本（MySQL 8+）
CREATE DATABASE IF NOT EXISTS ea_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ea_db;

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL COMMENT '用户ID',
    username      VARCHAR(64)  NOT NULL COMMENT '用户名',
    password      VARCHAR(128) NOT NULL COMMENT '密码',
    nickname      VARCHAR(64)           COMMENT '昵称',
    email         VARCHAR(128)          COMMENT '邮箱',
    phone         VARCHAR(32)           COMMENT '手机号',
    avatar        VARCHAR(255)          COMMENT '头像',
    gender        TINYINT      DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    status        TINYINT      DEFAULT 0 COMMENT '状态：0正常 1停用',
    dept_id       BIGINT                COMMENT '部门ID',
    remark        VARCHAR(500)          COMMENT '备注',
    create_by     VARCHAR(64)           COMMENT '创建人',
    create_time   DATETIME              COMMENT '创建时间',
    update_by     VARCHAR(64)           COMMENT '更新人',
    update_time   DATETIME              COMMENT '更新时间',
    deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 默认管理员密码：admin123（应用启动时也会自动初始化）
-- INSERT 可由 DataInitializer 完成，此处仅建表
