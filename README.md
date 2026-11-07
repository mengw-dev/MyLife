# MyLife

记录生活，积累知识，回顾成长。

MyLife 是一个以**人生时间线**为核心的个人成长记录系统：把日记、知识笔记、学习记录、重要事件与照片统一收纳在同一条时间线上，让你随时回到过去的任何一天。

## 仓库结构

| 目录 | 说明 |
| --- | --- |
| [backend](backend/) | 后端（Java 21 · Spring Boot 3 · Spring Security 6 · MyBatis-Plus · MySQL 8 · Flyway），详见 [backend/README.md](backend/README.md) |
| [frontend](frontend/) | 前端（Vue 3 · TypeScript · Vite · Pinia · Element Plus），详见 [frontend/README.md](frontend/README.md) |
| [docs](docs/) | 产品需求 / API 接口 / 数据库设计 / 开发规范四份基线文档 |

## 快速开始

**后端**（需 JDK 21 与 MySQL 8，表结构由 Flyway 自动迁移）：

```bash
cd backend
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
```

- 应用：`http://localhost:8080`（默认端口）
- 接口文档（Swagger）：`http://localhost:8080/swagger-ui.html`
- 数据库连接通过 `DB_USERNAME` / `DB_PASSWORD` 环境变量配置

**前端**（需 Node.js 18+）：

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

- 默认 Mock 模式（`VITE_USE_MOCK=true`），演示账号：`demo / 12345678`
- 连接真实后端：将 `frontend/.env.development` 中 `VITE_USE_MOCK` 改为 `false` 即可（dev 代理已指向 8080）

## 测试

```bash
cd backend
mvnw.cmd clean verify   # 34 个集成测试，跑在独立测试库 mylife_test
```

## Roadmap

- 时间线聚合接口（年份 / 年度 / 月份 / 日期）
- 跨模块搜索
- Markdown / JSON 导出与完整备份

## License

[MIT](LICENSE)