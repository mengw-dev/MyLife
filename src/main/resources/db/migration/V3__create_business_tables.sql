-- ============================================================
-- V3：按《03-数据库设计文档》创建 18 张业务表
-- 约定：BIGINT 主键自增；DATETIME(3) 审计字段（created_at / updated_at）；
--      所有个人业务数据通过 user_id 关联 t_user；
--      关联表外键：业务侧 ON DELETE CASCADE，媒体侧 ON DELETE RESTRICT（引用保护）；
--      关联关系的跨用户归属由 Service 层校验（外键只保证 ID 存在）。
-- 注意：MySQL 建表时被引用表必须已存在，故 media_file / tag 最先创建。
-- ============================================================

-- ---------- 媒体文件（被 life_stage / year_profile 封面引用，需先建） ----------
CREATE TABLE IF NOT EXISTS media_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL                COMMENT '所属用户',
    storage_key   VARCHAR(255) NOT NULL                COMMENT '服务端生成的文件存储标识',
    original_name VARCHAR(255) NOT NULL                COMMENT '原始文件名',
    mime_type     VARCHAR(100) NOT NULL                COMMENT '文件 MIME',
    size_bytes    BIGINT       NOT NULL                COMMENT '文件大小',
    sha256        CHAR(64)     NOT NULL                COMMENT '文件校验值',
    width         INT          DEFAULT NULL            COMMENT '图片宽度',
    height        INT          DEFAULT NULL            COMMENT '图片高度',
    description   VARCHAR(500) DEFAULT NULL            COMMENT '照片描述',
    created_at    DATETIME(3)  NOT NULL                COMMENT '上传时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_storage_key (storage_key),
    KEY idx_media_user_created (user_id, created_at),
    CONSTRAINT fk_media_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='照片及媒体文件';

-- ---------- 标签（被各标签关联表引用，需先建） ----------
CREATE TABLE IF NOT EXISTS tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT      NOT NULL                COMMENT '所属用户',
    name       VARCHAR(50) NOT NULL                COMMENT '标签名称',
    created_at DATETIME(3) NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_user_name (user_id, name),
    CONSTRAINT fk_tag_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='标签';

-- ---------- 人生阶段 ----------
CREATE TABLE IF NOT EXISTS life_stage (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT       NOT NULL                COMMENT '所属用户',
    name           VARCHAR(100) NOT NULL                COMMENT '阶段名称',
    start_date     DATE         NOT NULL                COMMENT '开始日期',
    end_date       DATE         DEFAULT NULL            COMMENT '结束日期，可空（空表示持续中）',
    description    TEXT                                 COMMENT '阶段介绍',
    cover_media_id BIGINT       DEFAULT NULL            COMMENT '封面照片，可空',
    sort_order     INT          NOT NULL DEFAULT 0      COMMENT '排序',
    created_at     DATETIME(3)  NOT NULL                COMMENT '创建时间',
    updated_at     DATETIME(3)  NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_stage_user_date (user_id, start_date, end_date),
    CONSTRAINT fk_stage_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_stage_cover FOREIGN KEY (cover_media_id) REFERENCES media_file (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='人生阶段';

-- ---------- 年度资料 ----------
CREATE TABLE IF NOT EXISTS year_profile (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT       NOT NULL                COMMENT '所属用户',
    year           SMALLINT     NOT NULL                COMMENT '年份',
    title          VARCHAR(120) DEFAULT NULL            COMMENT '年度标题',
    summary        TEXT                                 COMMENT '年度简介',
    cover_media_id BIGINT       DEFAULT NULL            COMMENT '代表照片',
    created_at     DATETIME(3)  NOT NULL                COMMENT '创建时间',
    updated_at     DATETIME(3)  NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_year_profile (user_id, year),
    CONSTRAINT fk_year_profile_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_year_profile_cover FOREIGN KEY (cover_media_id) REFERENCES media_file (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='年度资料';

-- ---------- 日记 ----------
CREATE TABLE IF NOT EXISTS diary (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL                COMMENT '所属用户',
    stage_id    BIGINT       DEFAULT NULL            COMMENT '人生阶段，可空',
    title       VARCHAR(200) NOT NULL                COMMENT '标题',
    content     LONGTEXT                             COMMENT '正文',
    record_date DATE         NOT NULL                COMMENT '记录日期',
    created_at  DATETIME(3)  NOT NULL                COMMENT '创建时间',
    updated_at  DATETIME(3)  NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_diary_user_date (user_id, record_date, id),
    KEY idx_diary_user_stage_date (user_id, stage_id, record_date),
    CONSTRAINT fk_diary_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_diary_stage FOREIGN KEY (stage_id) REFERENCES life_stage (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='日记';

-- ---------- 知识分类 ----------
CREATE TABLE IF NOT EXISTS knowledge_category (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL                COMMENT '所属用户',
    parent_id  BIGINT       DEFAULT NULL            COMMENT '父分类，可空',
    name       VARCHAR(100) NOT NULL                COMMENT '分类名称',
    sort_order INT          NOT NULL DEFAULT 0      COMMENT '排序',
    created_at DATETIME(3)  NOT NULL                COMMENT '创建时间',
    updated_at DATETIME(3)  NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_category_user_parent (user_id, parent_id, sort_order),
    CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='知识分类';

-- ---------- 知识笔记 ----------
CREATE TABLE IF NOT EXISTS knowledge_note (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT       NOT NULL                COMMENT '所属用户',
    category_id      BIGINT       NOT NULL                COMMENT '所属分类',
    title            VARCHAR(200) NOT NULL                COMMENT '标题',
    content_markdown LONGTEXT                             COMMENT 'Markdown 内容',
    created_at       DATETIME(3)  NOT NULL                COMMENT '创建时间',
    updated_at       DATETIME(3)  NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_note_user_category (user_id, category_id, updated_at),
    CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_note_category FOREIGN KEY (category_id) REFERENCES knowledge_category (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='知识笔记';

-- ---------- 学习记录 ----------
CREATE TABLE IF NOT EXISTS study_record (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id             BIGINT       NOT NULL                COMMENT '所属用户',
    note_id             BIGINT       DEFAULT NULL            COMMENT '关联知识笔记，可空',
    note_title_snapshot VARCHAR(200) DEFAULT NULL            COMMENT '笔记标题快照（笔记删除后保留）',
    stage_id            BIGINT       DEFAULT NULL            COMMENT '人生阶段，可空',
    title               VARCHAR(200) DEFAULT NULL            COMMENT '学习标题',
    content             LONGTEXT                             COMMENT '学习内容',
    understanding       LONGTEXT                             COMMENT '当时理解',
    questions           LONGTEXT                             COMMENT '遇到的问题',
    study_date          DATE         NOT NULL                COMMENT '学习日期',
    created_at          DATETIME(3)  NOT NULL                COMMENT '创建时间',
    updated_at          DATETIME(3)  NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_study_user_date (user_id, study_date, id),
    KEY idx_study_user_note (user_id, note_id, study_date),
    CONSTRAINT fk_study_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_study_note FOREIGN KEY (note_id) REFERENCES knowledge_note (id) ON DELETE SET NULL,
    CONSTRAINT fk_study_stage FOREIGN KEY (stage_id) REFERENCES life_stage (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='学习记录';

-- ---------- 重要事件 ----------
CREATE TABLE IF NOT EXISTS important_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL                COMMENT '所属用户',
    stage_id    BIGINT       DEFAULT NULL            COMMENT '人生阶段，可空',
    diary_id    BIGINT       DEFAULT NULL            COMMENT '关联日记，可空',
    title       VARCHAR(200) NOT NULL                COMMENT '事件标题',
    description LONGTEXT                             COMMENT '事件描述',
    event_date  DATE         NOT NULL                COMMENT '事件日期',
    created_at  DATETIME(3)  NOT NULL                COMMENT '创建时间',
    updated_at  DATETIME(3)  NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_event_user_date (user_id, event_date, id),
    KEY idx_event_user_stage (user_id, stage_id, event_date),
    CONSTRAINT fk_event_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_event_stage FOREIGN KEY (stage_id) REFERENCES life_stage (id) ON DELETE SET NULL,
    CONSTRAINT fk_event_diary FOREIGN KEY (diary_id) REFERENCES diary (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='重要事件';

-- ---------- 照片关联表（业务侧 CASCADE，媒体侧 RESTRICT 防误删） ----------
CREATE TABLE IF NOT EXISTS diary_media (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    diary_id   BIGINT      NOT NULL                COMMENT '日记 ID',
    media_id   BIGINT      NOT NULL                COMMENT '照片 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_diary_media (diary_id, media_id),
    CONSTRAINT fk_diary_media_diary FOREIGN KEY (diary_id) REFERENCES diary (id) ON DELETE CASCADE,
    CONSTRAINT fk_diary_media_media FOREIGN KEY (media_id) REFERENCES media_file (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='日记照片关联';

CREATE TABLE IF NOT EXISTS life_stage_media (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    stage_id   BIGINT      NOT NULL                COMMENT '阶段 ID',
    media_id   BIGINT      NOT NULL                COMMENT '照片 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stage_media (stage_id, media_id),
    CONSTRAINT fk_stage_media_stage FOREIGN KEY (stage_id) REFERENCES life_stage (id) ON DELETE CASCADE,
    CONSTRAINT fk_stage_media_media FOREIGN KEY (media_id) REFERENCES media_file (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='阶段照片关联';

CREATE TABLE IF NOT EXISTS knowledge_note_media (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    note_id    BIGINT      NOT NULL                COMMENT '笔记 ID',
    media_id   BIGINT      NOT NULL                COMMENT '照片 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_media (note_id, media_id),
    CONSTRAINT fk_note_media_note FOREIGN KEY (note_id) REFERENCES knowledge_note (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_media_media FOREIGN KEY (media_id) REFERENCES media_file (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='知识笔记照片关联';

CREATE TABLE IF NOT EXISTS event_media (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id   BIGINT      NOT NULL                COMMENT '事件 ID',
    media_id   BIGINT      NOT NULL                COMMENT '照片 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_media (event_id, media_id),
    CONSTRAINT fk_event_media_event FOREIGN KEY (event_id) REFERENCES important_event (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_media_media FOREIGN KEY (media_id) REFERENCES media_file (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='事件照片关联';

-- ---------- 标签关联表（两侧 CASCADE） ----------
CREATE TABLE IF NOT EXISTS diary_tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    diary_id   BIGINT      NOT NULL                COMMENT '日记 ID',
    tag_id     BIGINT      NOT NULL                COMMENT '标签 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_diary_tag (diary_id, tag_id),
    CONSTRAINT fk_diary_tag_diary FOREIGN KEY (diary_id) REFERENCES diary (id) ON DELETE CASCADE,
    CONSTRAINT fk_diary_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='日记标签关联';

CREATE TABLE IF NOT EXISTS knowledge_note_tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    note_id    BIGINT      NOT NULL                COMMENT '笔记 ID',
    tag_id     BIGINT      NOT NULL                COMMENT '标签 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_tag (note_id, tag_id),
    CONSTRAINT fk_note_tag_note FOREIGN KEY (note_id) REFERENCES knowledge_note (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='知识笔记标签关联';

CREATE TABLE IF NOT EXISTS study_record_tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    record_id  BIGINT      NOT NULL                COMMENT '学习记录 ID',
    tag_id     BIGINT      NOT NULL                COMMENT '标签 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_study_tag (record_id, tag_id),
    CONSTRAINT fk_study_tag_record FOREIGN KEY (record_id) REFERENCES study_record (id) ON DELETE CASCADE,
    CONSTRAINT fk_study_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='学习记录标签关联';

CREATE TABLE IF NOT EXISTS important_event_tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id   BIGINT      NOT NULL                COMMENT '事件 ID',
    tag_id     BIGINT      NOT NULL                COMMENT '标签 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_tag (event_id, tag_id),
    CONSTRAINT fk_event_tag_event FOREIGN KEY (event_id) REFERENCES important_event (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='事件标签关联';

CREATE TABLE IF NOT EXISTS media_tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    media_id   BIGINT      NOT NULL                COMMENT '照片 ID',
    tag_id     BIGINT      NOT NULL                COMMENT '标签 ID',
    created_at DATETIME(3) NOT NULL                COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_tag (media_id, tag_id),
    CONSTRAINT fk_media_tag_media FOREIGN KEY (media_id) REFERENCES media_file (id) ON DELETE CASCADE,
    CONSTRAINT fk_media_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='照片标签关联';