# MyLife — 个人生活与知识成长记录系统

> 后端工程已对齐四份基线文档（docs/01~04）：统一响应协议（code 0/40001）、/api/v1 前缀、Session + CSRF 认证、
> Base CRUD 全部完成（阶段 / 日记 / 知识库 / 学习记录 / 事件 / 照片 / 年度资料）。
> **时间线、搜索、数据导出与备份** 由你自己开发（接口契约见 docs/02 第 11-13 节）。

## 一、技术栈

| 项目 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring Security | Session 认证 + CSRF（Cookie 模式） |
| MyBatis-Plus | 3.5.18 + mybatis-plus-jsqlparser |
| Flyway | 数据库迁移（V1 → V3） |
| MySQL | 8.x |
| springdoc-openapi | 2.8.9（Swagger UI：/swagger-ui.html） |
| 测试 | JUnit 5 + spring-security-test（34 个集成测试） |

## 二、目录结构

```
src/main/java/cn/mengw21/mylife/
├── MyLifeApplication.java
├── common/                      # 公共组件
│   ├── api/                     # ApiResponse / PageResponse / IdVO
│   ├── entity/BaseEntity.java   # id + createdAt / updatedAt（UTC，自动填充）
│   ├── exception/               # BusinessException / GlobalExceptionHandler
│   └── service/                 # TagService / MediaOwnershipGuard（跨模块复用）
├── config/                      # Security / CSRF / Jackson / MyBatis-Plus / OpenAPI / CORS
├── security/                    # CurrentUser / DbUserDetailsService / LoginUser / 防暴力登录限流
└── module/
    ├── auth/                    # 注册 / 登录 / 登出 / me / 改密 / 注销账号（清全部数据+文件）
    ├── stage/                   # 人生阶段（CRUD + 排序 + 照片关联）
    ├── diary/                   # 日记（CRUD + 筛选分页 + 标签/照片）
    ├── knowledge/
    │   ├── category/            # 知识分类（多级树 + 防环 + 删除保护）
    │   └── note/                # 知识笔记（CRUD + 历史学习记录）
    ├── study/                   # 学习记录（CRUD + 笔记标题快照）
    ├── event/                   # 重要事件（CRUD + 日记/阶段归属校验）
    ├── media/                   # 照片（上传校验/鉴权读取/引用保护删除）
    ├── tag/                     # 标签（实体/Mapper，TagService 复用）
    ├── yearprofile/             # 年度资料（列表 + upsert + 删除）
    └── health/                  # 401 验证端点（开发辅助）

src/main/resources/db/migration/
├── V1__init.sql                        # 初始结构（已执行，禁止修改）
├── V2__align_user_and_drop_workspace.sql   # 对齐 t_user + 移除 workspace（PRD: V1 不做多人协作）
└── V3__create_business_tables.sql      # 18 张业务表（外键/索引按《03-数据库设计文档》）
```

## 三、统一响应协议（对齐 docs/02 §1.5）

```json
// 成功：HTTP 201，code 0
{ "code": 0, "message": "success", "data": { "id": "10001" } }

// 参数校验失败：HTTP 400，code 40001
{ "code": 40001, "message": "参数校验失败", "data": { "fieldErrors": [ { "field": "recordDate", "message": "记录日期不能为空" } ] } }

// 未登录 40101 / 无权或 CSRF 失败 40301 / 资源不存在或不属于当前用户 40401 / 业务冲突 40901
// 文件过大 41301 / 文件类型不支持 41501 / 系统异常 50000
```

约定：业务 code 前 3 位即 HTTP 状态码语义；ID 统一为字符串（防 JS 精度丢失）；时间戳 UTC ISO-8601（`2026-11-06T10:30:00Z`）；业务日期 `LocalDate`。

## 四、关键业务规则已实现（集成测试覆盖）

- 不同用户访问彼此资源统一 404（不暴露存在性）；关联对象（阶段/笔记/日记/照片/标签）一律校验归属。
- 删除阶段 / 知识笔记 / 日记 → 关联记录保留并解除关联（外键 SET NULL），学习记录保留笔记标题快照。
- 分类存在子分类或笔记时删除返回 409；父类别挂到自己子树下返回 400（防环）。
- 照片：魔数校验真实类型（JPEG/PNG/WebP/GIF ≤10MB）、SHA-256 校验值、私有目录 UUID 命名、被引用删除返回 409、原图必须登录后经接口读取。
- 注销账号：按外键顺序清理全部个人数据与媒体文件后删除账号。
- 登录防暴力：同用户名窗口内连续失败 5 次冷却 15 分钟。

## 五、快速启动 / 测试

```bash
# 开发（默认 dev，连本机 MySQL 8 root/1234；Flyway 自动迁移）
./mvnw.cmd spring-boot:run

# 完整集成测试（独立测试库 mylife_test，不碰开发库）
./mvnw.cmd clean verify
```

- Swagger UI：http://localhost:8080/swagger-ui.html
- CSRF：写请求前先 `GET /api/v1/auth/csrf`，把 Cookie `XSRF-TOKEN` 的值放进 `X-XSRF-TOKEN` 请求头。

## 六、接口一览（前缀 /api/v1，除注册/登录/CSRF 外全部需登录）

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | /auth/csrf、/auth/register、/auth/login、/auth/logout、/auth/me、/auth/password、/account | 见 docs/02 §2 |
| 人生阶段 | /life-stages（+ /order、/{id}/media） | docs/02 §3 |
| 日记 | /diaries | 分页筛选取自 docs/02 §4 |
| 知识分类 | /knowledge/categories | docs/02 §5 |
| 知识笔记 | /knowledge/notes（+ /{id}/study-records） | docs/02 §6 |
| 学习记录 | /study-records | docs/02 §7 |
| 重要事件 | /events | docs/02 §8 |
| 照片 | /media（+ /{id}/content） | docs/02 §9 |
| 年度资料 | /year-profiles（PUT/DELETE /{year}） | docs/02 §10 |

> 写请求（POST/PUT/DELETE）需带 CSRF Token；分页默认 20 条、最大 100 条。

## 七、下一步（你自己开发）

按 docs/02 §11-13 增补：`/timeline/**`（时间线四级聚合）、`/search`（跨模块搜索）、
`/exports/**` 与 `/backups/**`（导出 / 备份 / 恢复）。开发时遵守 docs/04 的 Agent 协作规范：
每次只做一个明确模块、附带测试、不擅改数据库与公共 API。

## 八、前后端契约注意事项（踩坑记录）

1. **springdoc 版本**：springdoc 3.x 面向 Spring Boot 4，Boot 3.5 项目必须用 2.8.x。
2. **MyBatis-Plus 置空更新**：`updateById` 默认忽略 null 字段，可空关联字段需加
   `@TableField(updateStrategy = FieldStrategy.ALWAYS)`，否则"解除关联"不会生效。
3. **Flyway 建表顺序**：MySQL 建表时被外键引用的表必须已存在（否则 1824），媒体/标签表放在 V3 最前。
4. **测试库迁移失败后**：V3 等迁移失败会在 schema_history 留下失败记录，重建测试库即可
   （`DROP DATABASE mylife_test`）。