/**
 * 核心业务实体类型（与后端 docs/03 对齐；ID 一律为字符串）
 * 各模块可在 modules/<领域>/types 延伸 DTO / 视图类型，公共实体统一放这里。
 */

export interface User {
  id: string
  username: string
  nickname: string
  createdAt: string
}

export interface LifeStage {
  id: string
  name: string
  startDate: string
  endDate: string | null
  description: string | null
  coverMediaId: string | null
  sortOrder: number
  mediaIds?: string[]
  createdAt: string
  updatedAt: string
}

export interface YearProfile {
  id: string
  year: number
  title: string | null
  summary: string | null
  coverMediaId: string | null
  createdAt: string
  updatedAt: string
}

export interface Diary {
  id: string
  title: string
  content: string
  recordDate: string
  stageId: string | null
  tagNames: string[]
  mediaIds: string[]
  createdAt: string
  updatedAt: string
}

export interface KnowledgeCategory {
  id: string
  name: string
  parentId: string | null
  sortOrder: number
  children: KnowledgeCategory[]
}

export interface KnowledgeNote {
  id: string
  title: string
  contentMarkdown: string
  categoryId: string
  tagNames: string[]
  mediaIds: string[]
  createdAt: string
  updatedAt: string
}

export interface StudyRecord {
  id: string
  title: string | null
  content: string
  understanding: string | null
  questions: string | null
  studyDate: string
  noteId: string | null
  noteTitleSnapshot: string | null
  stageId: string | null
  tagNames: string[]
  createdAt: string
  updatedAt: string
}

export interface ImportantEvent {
  id: string
  title: string
  eventDate: string
  description: string | null
  stageId: string | null
  diaryId: string | null
  mediaIds: string[]
  tagNames: string[]
  createdAt: string
  updatedAt: string
}

export interface MediaFile {
  id: string
  originalName: string
  mimeType: string
  sizeBytes: number
  description: string | null
  width: number | null
  height: number | null
  createdAt: string
}