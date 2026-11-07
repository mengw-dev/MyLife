# MyLife

记录生活，积累知识，回顾成长。一个以人生时间线为核心的个人成长记录系统，统一收纳日记、知识、学习与重要事件。

## 功能

- 人生阶段、日记、知识库（分类 + 笔记 + 学习记录）、重要事件、相册、年度资料
- Session 登录、BCrypt、CSRF、数据隔离、防暴力登录、账号注销一键清理

## 技术栈

Java 21 · Spring Boot 3 · Spring Security 6 · MyBatis-Plus · MySQL 8 · Flyway · springdoc-openapi · JUnit 5

## 快速开始

需要 JDK 21 与 MySQL 8。

```bash
mvnw.cmd spring-boot:run   # Windows；Linux/macOS 使用 ./mvnw spring-boot:run
```

- 应用：http://localhost:8080
- 接口文档：http://localhost:8080/swagger-ui.html
- 数据库连接通过 `DB_USERNAME` / `DB_PASSWORD` 环境变量配置，表结构由 Flyway 自动迁移

## 测试

```bash
mvnw.cmd clean verify
```

## 文档

需求、接口、数据库、开发规范见 [docs](docs/)。

## Roadmap

时间线聚合 · 跨模块搜索 · 数据导出与备份

## License

[MIT](LICENSE)