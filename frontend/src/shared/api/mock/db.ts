/**
 * Mock 内存数据库：所有模拟接口共享同一份数据，行为尽量贴近后端语义。
 * 不是正式业务库——仅供 VITE_USE_MOCK=true 时使用（见 DEV-NOTES §3）。
 */
import type {
  Diary, ImportantEvent, KnowledgeCategory, KnowledgeNote, LifeStage,
  MediaFile, StudyRecord, User, YearProfile,
} from '@/shared/types/entities'

export interface MockDb {
  users: User[]
  userPasswords: Record<string, string>
  stages: LifeStage[]
  yearProfiles: YearProfile[]
  diaries: Diary[]
  categories: KnowledgeCategory[]
  notes: KnowledgeNote[]
  records: StudyRecord[]
  events: ImportantEvent[]
  medias: MediaFile[]
}

let seq = 1000
export function nextId(): string {
  seq += 1
  return String(seq)
}

const now = (daysAgo = 0) => new Date(Date.now() - daysAgo * 86400000).toISOString()
const date = (offsetDays = 0) => {
  const d = new Date(Date.now() - offsetDays * 86400000)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function createMockDb(): MockDb {
  return {
    users: [{ id: '1', username: 'demo', nickname: '小满', createdAt: now(900) }],
    userPasswords: { demo: '12345678' },

    stages: [
      { id: '101', name: '大学', startDate: '2021-09-01', endDate: '2025-06-30', description: '在南京的四年', coverMediaId: null, sortOrder: 1, createdAt: now(700), updatedAt: now(700) },
      { id: '102', name: '职场初年', startDate: '2025-07-01', endDate: null, description: '成为工程师的第一段旅程', coverMediaId: null, sortOrder: 2, createdAt: now(200), updatedAt: now(200) },
    ],

    yearProfiles: [
      { id: '201', year: 2023, title: '技术探索', summary: '开始系统学习 Java 与后端工程。', coverMediaId: null, createdAt: now(600), updatedAt: now(600) },
      { id: '202', year: 2024, title: '成长与积累', summary: '搭建个人项目，建立知识库习惯。', coverMediaId: null, createdAt: now(300), updatedAt: now(300) },
      { id: '203', year: 2025, title: '新的开始', summary: '毕业，进入职场。', coverMediaId: null, createdAt: now(120), updatedAt: now(120) },
    ],

    diaries: [
      { id: '301', title: '第一次配置 Spring Boot', content: '今天跑通了第一个 Spring Boot 应用，Flyway 自动建表真好用。', recordDate: '2023-03-12', stageId: '101', tagNames: ['Java', '后端'], mediaIds: [], createdAt: now(700), updatedAt: now(700) },
      { id: '302', title: '读完《人月神话》', content: '没有银弹。复杂问题的本质是沟通与设计。', recordDate: '2024-01-20', stageId: '101', tagNames: ['阅读'], mediaIds: [], createdAt: now(500), updatedAt: now(500) },
      { id: '303', title: '毕业答辩通过', content: '四年结束了，比想象中平静。', recordDate: '2025-06-18', stageId: '101', tagNames: ['里程碑'], mediaIds: [], createdAt: now(180), updatedAt: now(180) },
      { id: '304', title: '入职第一天', content: '认识了一群很有意思的同事。', recordDate: '2025-07-07', stageId: '102', tagNames: ['工作'], mediaIds: [], createdAt: now(160), updatedAt: now(160) },
      { id: '305', title: '周复盘', content: '本周：完成数据库基线对齐，前端时间线开始动工。', recordDate: date(1), stageId: '102', tagNames: ['复盘'], mediaIds: [], createdAt: now(1), updatedAt: now(1) },
      { id: '306', title: '今天的记录', content: '学习 MyBatis-Plus 自动填充与置空更新策略。', recordDate: date(0), stageId: '102', tagNames: ['Java'], mediaIds: [], createdAt: now(0), updatedAt: now(0) },
    ],

    categories: [
      { id: '401', name: '计算机', parentId: null, sortOrder: 1, children: [] },
      { id: '402', name: 'Java', parentId: '401', sortOrder: 1, children: [] },
      { id: '403', name: 'MySQL', parentId: '401', sortOrder: 2, children: [] },
      { id: '404', name: '其他', parentId: null, sortOrder: 2, children: [] },
      { id: '405', name: '英语', parentId: '404', sortOrder: 1, children: [] },
    ],

    notes: [
      { id: '501', title: 'Java 线程池', contentMarkdown: '# Java 线程池\n\n## 核心参数\n\n- corePoolSize 核心线程数\n- maximumPoolSize 最大线程数\n- workQueue 任务队列\n\n```java\nExecutorService pool = Executors.newFixedThreadPool(4);\n```', categoryId: '402', tagNames: ['并发'], mediaIds: [], createdAt: now(90), updatedAt: now(20) },
      { id: '502', title: 'MySQL 索引', contentMarkdown: '# 索引\n\n最左前缀原则：\n\n1. 等值条件放最前\n2. 范围条件放最后\n\n> 覆盖索引可以避免回表', categoryId: '403', tagNames: ['数据库'], mediaIds: [], createdAt: now(60), updatedAt: now(60) },
    ],

    records: [
      { id: '601', title: '初次学习线程池', content: '第一次接触线程池概念。', understanding: '理解了为什么需要复用线程。', questions: '拒绝策略到底怎么选？', studyDate: '2024-03-05', noteId: '501', noteTitleSnapshot: 'Java 线程池', stageId: '101', tagNames: ['Java'], createdAt: now(80), updatedAt: now(80) },
      { id: '602', title: '理解核心参数', content: '梳理了七大参数的含义。', understanding: 'core 与 max 的区别清楚了。', questions: null, studyDate: '2024-03-08', noteId: '501', noteTitleSnapshot: 'Java 线程池', stageId: '101', tagNames: ['Java'], createdAt: now(77), updatedAt: now(77) },
      { id: '603', title: '最左前缀实践', content: '用 explain 验证了联合索引。', understanding: '范围条件导致后续索引失效。', questions: null, studyDate: '2024-05-11', noteId: '502', noteTitleSnapshot: 'MySQL 索引', stageId: '101', tagNames: ['数据库'], createdAt: now(60), updatedAt: now(60) },
      { id: '604', title: '复盘索引设计', content: '给 diary 表设计了 (user_id, record_date) 索引。', understanding: null, questions: null, studyDate: date(0), noteId: null, noteTitleSnapshot: null, stageId: '102', tagNames: [], createdAt: now(0), updatedAt: now(0) },
    ],

    events: [
      { id: '701', title: '第一次实习', eventDate: '2023-07-01', description: '在一个小团队做 Java 后端。', stageId: '101', diaryId: null, mediaIds: [], tagNames: ['工作'], createdAt: now(650), updatedAt: now(650) },
      { id: '702', title: '毕业', eventDate: '2025-06-20', description: '学士学位，南京。', stageId: null, diaryId: '303', mediaIds: [], tagNames: ['里程碑'], createdAt: now(170), updatedAt: now(170) },
      { id: '703', title: '入职', eventDate: '2025-07-07', description: '后端工程师。', stageId: '102', diaryId: '304', mediaIds: [], tagNames: ['工作'], createdAt: now(160), updatedAt: now(160) },
    ],

    medias: [
      { id: '801', originalName: 'campus.jpg', mimeType: 'image/jpeg', sizeBytes: 128000, description: '校园的秋天', width: 640, height: 420, createdAt: now(600) },
      { id: '802', originalName: 'sunset.png', mimeType: 'image/png', sizeBytes: 256000, description: '入职那天傍晚', width: 640, height: 420, createdAt: now(159) },
    ],
  }
}

/** 全局唯一 Mock 库实例：所有模块的 mock 处理器共享同一份数据 */
export const mockDb: MockDb = createMockDb()