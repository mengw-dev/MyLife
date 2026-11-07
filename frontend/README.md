# MyLife 前端

以「人生时间线」为核心的个人成长记录系统 Web 前端（Vue 3 + TypeScript + Vite + Pinia + Element Plus + Axios）。

## 快速开始

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # vue-tsc 类型检查 + 产物构建（验收标准）
```

### Mock 与真实后端切换

| 场景 | 做法 |
| --- | --- |
| 无后端，本地演示（默认） | `.env.development` 中 `VITE_USE_MOCK=true`，所有数据来自内存种子库（登录：demo / 12345678） |
| 连接真实后端 | 改为 `VITE_USE_MOCK=false`。vite 已代理 `/api` → `http://localhost:8080`，Session Cookie 同源携带；写请求自动附带 `X-XSRF-TOKEN`（CSRF 流程见 docs/02） |

## 目录结构

```text
src/
├── app/            # App.vue / main.ts
├── router/         # routes.ts / guards.ts（登录校验集中在此）
├── layouts/        # MainLayout（主时间线）/ ManageLayout（管理中心三栏 + 右侧实时预览）
├── modules/        # 与后端模块一一对应（auth/timeline/diary/knowledge/study/life-stage/event/media/search/data-management/settings）
├── shared/         # http/CSRF 封装、Mock 适配层、实体类型、公共组件、工具
├── stores/         # auth（用户态）、preview（草稿预览状态机）
└── styles/         # variables.css 设计令牌、全局与过渡样式
```

## 关键机制

- **时间线四级缩放**：年份总览 → 年度 → 月份日历 → 日期详情，路由层级 + 方向过渡；聚合数据统一由 `modules/timeline` 提供，主时间线与管理预览复用同一份数据。
- **管理中心实时预览**：编辑表单变化即时写入 `stores/preview`（草稿），右侧 `TimelinePreview` 以虚线 +「预览」标签叠加显示；保存成功才提交后端并同步时间线；离页有未保存提示。
- **分层约束**：页面只组合组件；API 全部走 `shared/api/http`；模块间禁止循环依赖。详见 [DEV-NOTES.md](./DEV-NOTES.md)。

## 尚未对接的真实接口

- `/timeline/**`、`/search`、`/exports/**`、`/backups/**`：后端按 Roadmap 排期中，当前在 Mock 模式下以同一契约演示，后端就绪后将 `VITE_USE_MOCK=false` 即可切换。
- 照片上传：真实模式为 `multipart/form-data`（`/media`），Mock 模式以 JSON 元数据代替（无真实文件）。
- 昵称修改接口：后端未提供，设置页暂无此入口。