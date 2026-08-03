CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT       NOT NULL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    password      VARCHAR(128) NOT NULL,
    nickname      VARCHAR(64),
    email         VARCHAR(128),
    phone         VARCHAR(32),
    avatar        VARCHAR(255),
    gender        INT          DEFAULT 0,
    status        INT          DEFAULT 0,
    dept_id       BIGINT,
    remark        VARCHAR(500),
    create_by     VARCHAR(64),
    create_time   TIMESTAMP,
    update_by     VARCHAR(64),
    update_time   TIMESTAMP,
    deleted       INT          DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user (username);
