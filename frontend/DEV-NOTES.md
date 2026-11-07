# MyLife 前端开发约定（协作者必读）

> 本文档是前端长期架构约束，任何协作者（人或工具）新增代码前必须阅读。

## 1. 目录与模块边界

- `src/shared/`：跨模块公共能力（http、Mock、类型、组件、工具、常量）。只有真正跨业务复用的组件才允许放这里。
- `src/modules/<领域>/`：按后端模块一一对应（auth/timeline/diary/knowledge/study/life-stage/event/media/search/data-management）。
- 模块内部：`api/`（HTTP 与类型）、`components/`（模块内组件）、`composables/`（复杂逻辑）、`stores/`（跨组件状态，可省略）、`types/`、`views/`（页面只组合组件）。
- 禁止循环依赖；模块间通过 shared 类型 / API / 状态协作；管理中心预览复用 timeline 组件，不得另写一套聚合逻辑。
- 页面 ≤200 行，Vue 文件 >300 行必须拆分，>500 行禁止。

## 2. 技术约定

- Vue 3 `<script setup lang="ts">` + Composition API；TypeScript strict，禁止滥用 `any`；Props/Emits 显式声明。
- Pinia（stores/auth.ts 用户态；stores/preview.ts 草稿预览；timeline 数据在 modules/timeline/stores）。
- 样式：组件内 scoped CSS，公共变量用 `src/styles/variables.css` 的 CSS 变量，禁止魔法色值/魔法数字。
- 动画 180~280ms，遵守 prefers-reduced-motion。
- 路由全部懒加载，登录校验只在 guards.ts 一处。

## 3. API 约定（对齐后端 docs/02）

- 统一封装 `shared/api/http.ts`（axios：`http.get/post/put/del`），组件里禁止直接写 axios。
- 基础路径来自 `VITE_API_BASE`（默认 `/api/v1`）；Session Cookie 自动携带（withCredentials）；CSRF 自动处理（`X-XSRF-TOKEN` 头，仅 Mock 关闭时生效）。
- 响应统一解包 `ApiResponse<T> { code, message, data }`，业务失败自动 ElMessage 提示；401 自动跳登录。
- ID 一律是字符串（后端契约）；时间戳为 UTC ISO 字符串（`2026-11-06T10:00:00Z`），业务日期为 `YYYY-MM-DD`。
- `VITE_USE_MOCK=true` 时走 Mock 适配器：在 `shared/api/mock/registry.ts` 注册处理器，数据操作共享内存库 `shared/api/mock/db.ts`（初始化种子在 db.ts 内）。禁止组件内写死假数据，禁止用 localStorage 当业务库。

## 4. 设计语言（数字档案馆）

- 令牌已定义于 `src/styles/variables.css`：暖白底 `--bg`、卡片 `--surface`、主字 `--ink`、强调暖棕 `--accent`，语义标记 `--mark-diary/study/event`。
- 年份数字用衬线 `--font-serif`；卡片轻边框弱阴影；不要渐变堆叠、不要 SaaS 后台味。
- 时间线是产品门面，管理中心是工具，视觉重心永远在时间线。

## 5. 实时预览机制（重点）

- `stores/preview.ts` 是唯一草稿状态源：`setDraft(draft)` 覆盖预览，`commit()`（保存成功后）清除草稿并通知 timeline store 刷新，`discard()` 撤销。
- 编辑表单变更立即 `setDraft`（不请求后端），保存才调 API；保存失败保留草稿。
- 预览面板 `modules/timeline/components/TimelinePreview.vue` 复用 timeline 数据与组件，草稿以虚线边框 +「预览」标签区分。
- 离开含未保存修改的页面用路由守卫/SaveGuard 提示确认。

## 6. 验收

`npm run build` 必须通过（vue-tsc 严格检查）；页面无控制台报错；CRUD、加载/空/错误三态齐全；删除有确认；图片失败有占位。