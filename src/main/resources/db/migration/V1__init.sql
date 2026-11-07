-- ============================================================
-- MyLife 初始表结构
-- 说明：表名加 t_ 前缀，避免 user 等 MySQL 保留字冲突
-- ============================================================

-- 用户表
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL                COMMENT '登录名，唯一',
    password    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密文',
    nickname    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- 工作空间表
CREATE TABLE workspace (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    owner_id    BIGINT       NOT NULL                COMMENT '所有者用户 ID',
    name        VARCHAR(100) NOT NULL                COMMENT '空间名称',
    description VARCHAR(255) DEFAULT NULL            COMMENT '空间简介',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='工作空间表';

-- 工作空间成员表（owner 也是成员，role 区分）
CREATE TABLE workspace_member (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    workspace_id BIGINT      NOT NULL                COMMENT '空间 ID',
    user_id      BIGINT      NOT NULL                COMMENT '成员用户 ID',
    role         VARCHAR(20) NOT NULL                COMMENT '角色：OWNER / MEMBER',
    create_time  DATETIME    NOT NULL                COMMENT '加入时间',
    update_time  DATETIME    NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ws_user (workspace_id, user_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='工作空间成员表';