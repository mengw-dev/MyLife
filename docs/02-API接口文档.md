# MyLife API 接口文档

| 项目 | 内容 |
| --- | --- |
| 文档版本 | V1.0 |
| 接口前缀 | /api/v1 |
| 接口风格 | RESTful |
| 数据格式 | JSON |

## 1. 全局约定

### 1.1 基础信息

- Base URL: `/api/v1`
- Content-Type: `application/json`
- Authentication: Session

所有需要登录的接口通过 Session Cookie 认证。

不使用 JWT。

### 1.2 用户身份

所有业务接口禁止接收 `userId` 作为数据权限依据。

服务端从 Spring Security 获取当前登录用户 ID。

### 1.3 ID 格式

数据库主键使用 `BIGINT`。

JSON 中统一使用字符串传递 ID，避免 JavaScript 数字精度问题。

例如：

```json
{
  "id": "10001"
}
```

### 1.4 日期格式

业务日期：

```text
2026-11-06
```

时间戳：

```text
2026-11-06T10:30:00Z
```

业务日期使用 `LocalDate`，时间戳使用 UTC。

### 1.5 统一响应格式

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "page": 1,
    "pageSize": 20,
    "total": 0,
    "pages": 0
  }
}
```

失败：

```json
{
  "code": 40001,
  "message": "参数校验失败",
  "data": {
    "fieldErrors": [
      {
        "field": "recordDate",
        "message": "记录日期不能为空"
      }
    ]
  }
}
```

### 1.6 HTTP 状态码

| 状态码 | 含义 |
| --- | --- |
| 200 | 请求成功 |
| 201 | 创建成功 |
| 204 | 删除成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权操作或 CSRF 校验失败 |
| 404 | 资源不存在或不属于当前用户 |
| 409 | 业务冲突 |
| 413 | 文件过大 |
| 415 | 文件类型不支持 |
| 500 | 服务端异常 |

## 2. 用户认证接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /auth/csrf | 获取 CSRF Token |
| POST | /auth/register | 用户注册 |
| POST | /auth/login | 用户登录 |
| POST | /auth/logout | 用户登出 |
| GET | /auth/me | 获取当前用户 |
| PUT | /auth/password | 修改密码 |
| DELETE | /account | 注销账号 |

注册请求：

```json
{
  "username": "testuser",
  "password": "12345678",
  "nickname": "测试用户"
}
```

登录请求：

```json
{
  "username": "testuser",
  "password": "12345678"
}
```

当前用户响应：

```json
{
  "id": "1",
  "username": "testuser",
  "nickname": "测试用户",
  "createdAt": "2026-11-06T10:00:00Z"
}
```

## 3. 人生阶段接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /life-stages | 阶段列表 |
| POST | /life-stages | 创建阶段 |
| GET | /life-stages/{id} | 阶段详情 |
| PUT | /life-stages/{id} | 修改阶段 |
| DELETE | /life-stages/{id} | 删除阶段 |
| PUT | /life-stages/order | 批量调整顺序 |
| PUT | /life-stages/{id}/media | 设置阶段照片（V1.0 补充接口） |

创建请求：

```json
{
  "name": "大学生活",
  "startDate": "2023-09-01",
  "endDate": "2027-06-30",
  "description": "我的大学阶段",
  "coverMediaId": "100",
  "sortOrder": 1
}
```

设置阶段照片请求（对应数据库 `life_stage_media` 表，`mediaIds` 表示完整替换全部阶段照片关联）：

```json
{
  "mediaIds": ["100", "101"]
}
```

删除规则：关联的日记、学习记录和事件保留，`stage_id` 设置为空。

## 4. 日记接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /diaries | 日记分页查询 |
| POST | /diaries | 创建日记 |
| GET | /diaries/{id} | 日记详情 |
| PUT | /diaries/{id} | 修改日记 |
| DELETE | /diaries/{id} | 删除日记 |

列表查询参数：

- `page`
- `pageSize`
- `keyword`
- `from`
- `to`
- `stageId`
- `tag`

创建请求：

```json
{
  "title": "今天的记录",
  "content": "今天学习了 Spring Boot。",
  "recordDate": "2026-11-06",
  "stageId": "1",
  "tagNames": ["学习", "Java"],
  "mediaIds": ["100", "101"]
}
```

修改请求与创建请求使用相同结构。

`tagNames` 和 `mediaIds` 在 PUT 时表示完整替换对应关联集合。

## 5. 知识分类接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /knowledge/categories | 获取分类树 |
| POST | /knowledge/categories | 创建分类 |
| PUT | /knowledge/categories/{id} | 修改分类 |
| DELETE | /knowledge/categories/{id} | 删除分类 |

创建请求：

```json
{
  "name": "Java",
  "parentId": "1",
  "sortOrder": 1
}
```

删除规则：

存在子分类或知识笔记时返回 409。

## 6. 知识笔记接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /knowledge/notes | 笔记分页 |
| POST | /knowledge/notes | 创建笔记 |
| GET | /knowledge/notes/{id} | 笔记详情 |
| PUT | /knowledge/notes/{id} | 修改笔记 |
| DELETE | /knowledge/notes/{id} | 删除笔记 |
| GET | /knowledge/notes/{id}/study-records | 查看历史学习记录 |

创建请求：

```json
{
  "title": "Java 线程池",
  "contentMarkdown": "# Java 线程池\n\n线程池的核心参数...",
  "categoryId": "10",
  "tagNames": ["Java", "并发"],
  "mediaIds": []
}
```

删除笔记时：

- 保留学习记录。
- 将学习记录的 `note_id` 设置为空。
- 保留原笔记标题快照。

## 7. 学习记录接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /study-records | 学习记录分页 |
| POST | /study-records | 创建学习记录 |
| GET | /study-records/{id} | 学习记录详情 |
| PUT | /study-records/{id} | 修改学习记录 |
| DELETE | /study-records/{id} | 删除学习记录 |

查询参数：

- `page`
- `pageSize`
- `from`
- `to`
- `noteId`
- `stageId`
- `keyword`
- `tag`

创建请求：

```json
{
  "title": "学习线程池参数",
  "content": "今天学习了线程池的核心参数。",
  "understanding": "理解了核心线程数和最大线程数的区别。",
  "questions": "拒绝策略还需要进一步研究。",
  "studyDate": "2026-11-06",
  "noteId": "100",
  "stageId": "1",
  "tagNames": ["Java"]
}
```

## 8. 重要事件接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /events | 事件分页 |
| POST | /events | 创建事件 |
| GET | /events/{id} | 事件详情 |
| PUT | /events/{id} | 修改事件 |
| DELETE | /events/{id} | 删除事件 |

创建请求：

```json
{
  "title": "完成 MyLife 基础设计",
  "eventDate": "2026-11-06",
  "description": "完成个人生活记录系统的设计。",
  "stageId": "1",
  "diaryId": "100",
  "mediaIds": [],
  "tagNames": ["项目"]
}
```

## 9. 照片接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| POST | /media | 上传照片 |
| GET | /media | 照片列表 |
| GET | /media/{id} | 照片详情 |
| GET | /media/{id}/content | 获取照片原图 |
| DELETE | /media/{id} | 删除照片 |

上传使用 `multipart/form-data`。

上传字段：

- `file`

上传成功返回：

```json
{
  "id": "100",
  "originalName": "photo.jpg",
  "mimeType": "image/jpeg",
  "sizeBytes": 102400,
  "description": null
}
```

删除仍被引用的照片时返回 409。

## 10. 年度资料接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /year-profiles | 年度资料列表 |
| PUT | /year-profiles/{year} | 创建或修改年度资料 |
| DELETE | /year-profiles/{year} | 删除年度资料 |

请求：

```json
{
  "title": "技术探索与成长",
  "summary": "这一年开始系统学习 Java。",
  "coverMediaId": "100"
}
```

年度资料通过 `user_id + year` 唯一确定。

## 11. 时间线接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /timeline/years | 年份总览 |
| GET | /timeline/years/{year} | 年度详情 |
| GET | /timeline/months/{year}/{month} | 月份详情 |
| GET | /timeline/days/{date} | 日期详情 |

年份总览示例：

```text
GET /timeline/years?fromYear=2020&toYear=2030
```

返回每年的：

- 年份。
- 年度标题。
- 年度简介。
- 日记数量。
- 学习记录数量。
- 重要事件数量。
- 是否有记录。

月份详情：

```text
GET /timeline/months/2026/11
```

每一天返回：

```json
{
  "date": "2026-11-06",
  "diaryCount": 2,
  "studyCount": 3,
  "eventCount": 1,
  "hasRecords": true
}
```

日期详情：

```text
GET /timeline/days/2026-11-06
```

返回：

```json
{
  "date": "2026-11-06",
  "diaries": [],
  "studyRecords": [],
  "events": []
}
```

可选筛选参数：

```text
types=diary,study,event
```

## 12. 搜索接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| GET | /search | 跨模块搜索 |

参数：

- `q`
- `types`
- `from`
- `to`
- `stageId`
- `tag`
- `page`
- `pageSize`

支持搜索：

- 日记。
- 知识笔记。
- 学习记录。
- 重要事件。

返回统一结果：

```json
{
  "type": "diary",
  "id": "100",
  "title": "今天的记录",
  "date": "2026-11-06",
  "snippet": "今天学习了..."
}
```

## 13. 数据导出接口

| 方法 | 接口 | 功能 |
| --- | --- | --- |
| POST | /exports | 普通数据导出 |
| POST | /exports/ai-package | AI 分析资料包 |
| POST | /backups/export | 完整备份 |
| POST | /backups/validate | 备份文件预校验 |
| POST | /backups/restore | 恢复备份 |

普通导出请求：

```json
{
  "format": "markdown",
  "include": [
    "diaries",
    "knowledge",
    "studyRecords",
    "events",
    "stages"
  ],
  "from": "2026-01-01",
  "to": "2026-12-31"
}
```

格式支持：

- `markdown`
- `json`
- `zip`

完整备份 ZIP 结构：

```text
MyLife-Backup.zip
├── manifest.json
├── data.json
├── checksums.sha256
└── files/
    ├── image1.jpg
    ├── image2.png
    └── image3.webp
```

恢复必须先完成预校验，再允许用户确认替换数据。

## 14. API 统一开发要求

1. 所有业务接口必须检查登录状态。
2. 所有业务数据必须验证 `user_id`。
3. 所有详情、修改和删除接口都必须验证资源归属。
4. 所有写请求必须经过 CSRF 校验。
5. 所有请求参数必须进行校验。
6. 所有接口统一异常响应。
7. 分页默认 20 条，最大 100 条。
8. 所有多表写入必须考虑事务。
9. 文件上传必须检查真实文件类型。
10. 任何接口都不能返回密码哈希或服务器绝对路径。

**接口文档是前后端共同遵守的契约。接口发生变化时，必须同步修改本文档。**