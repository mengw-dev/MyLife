-- ============================================================
-- V2：对齐《03-数据库设计文档》基准结构
-- 1) t_user 按文档改名/补列（password_hash / created_at / status / last_login_at）
-- 2) 移除工作空间表（PRD 14 非目标：V1 不做多人协作）
-- ============================================================

-- 1. 列重命名
ALTER TABLE t_user
    RENAME COLUMN password TO password_hash,
    RENAME COLUMN create_time TO created_at,
    RENAME COLUMN update_time TO updated_at;

-- 2. 类型/长度对齐文档
ALTER TABLE t_user
    MODIFY COLUMN username VARCHAR(32) NOT NULL COMMENT '用户名，唯一',
    MODIFY COLUMN password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    MODIFY COLUMN nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    MODIFY COLUMN created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME(3) NOT NULL COMMENT '修改时间';

-- 3. 补充文档字段（email 为项目扩展字段，保留）
ALTER TABLE t_user
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：1 正常 / 0 注销或停用' AFTER nickname,
    ADD COLUMN last_login_at DATETIME(3) DEFAULT NULL COMMENT '最后登录时间' AFTER status;

-- 4. 移除工作空间（业务代码与测试已同步删除）
DROP TABLE IF EXISTS workspace_member;
DROP TABLE IF EXISTS workspace;