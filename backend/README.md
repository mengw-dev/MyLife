# MyLife 后端

MyLife 后端工程（Java 21 · Spring Boot 3），与根目录 `frontend/` 的 Web 前端配对。接口契约见根目录 `../docs/02-API接口文档.md`。

## 技术栈

Java 21 · Spring Boot 3.5 · Spring Security 6（Session + CSRF）· MyBatis-Plus 3.5 · MySQL 8 · Flyway · springdoc-openapi 2.8（Swagger UI）· JUnit 5

## 目录结构

```text
backend/src/main/java/cn/mengw21/mylife/
├── common/      # 统一响应（code 0/40001 体系）、异常处理、公共服务
├── config/      # Security / Jackson / MyBatis-Plus / OpenAPI / CORS
├── security/    # Session 认证、CSRF、防暴力登录
└── module/      # auth / stage / diary / knowledge / study / event / media / tag / yearprofile
```

数据库迁移：`backend/src/main/resources/db/migration/`（V1→V3，禁止回改已执行文件）。

## 快速开始

```bash
# 需 JDK 21 + MySQL 8（默认 root/1234，可用环境变量覆盖）
mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run        # Linux / macOS
```

- 应用：http://localhost:8080 ｜ Swagger：http://localhost:8080/swagger-ui.html
- Session Cookie 认证；写请求需先 `GET /api/v1/auth/csrf` 获取 CSRF Token 放入 `X-XSRF-TOKEN` 头
- 前端联调：frontend 的 vite dev 已代理 `/api` → `http://localhost:8080`

## 测试

```bash
mvnw.cmd clean verify   # 34 个集成测试，独立测试库 mylife_test
```

## 统一响应协议

`{ code, message, data }`：成功 `0`；参数错误 `40001`（含 `data.fieldErrors`）；未登录 `40101`；无权/CSRF `40301`；不存在或不属于当前用户 `40401`；冲突 `40901`；文件过大 `41301`；类型不支持 `41501`。ID 一律字符串，时间戳 UTC ISO-8601。

## 踩坑记录

1. springdoc 3.x 面向 Spring Boot 4，Boot 3.5 必须用 2.8.x。
2. MyBatis-Plus `updateById` 默认忽略 null 字段，可空关联字段需 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`。
3. Flyway 建表时被外键引用的表必须已存在（MySQL 1824），media/tag 表建在最前。
4. 迁移失败会在 `flyway_schema_history` 留下记录，重建测试库（`DROP DATABASE mylife_test`）即可。