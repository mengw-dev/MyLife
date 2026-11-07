/**
 * 学习记录 API 与请求/响应类型（对齐 docs/02 §7）。
 */
import { get, post, put, del } from '@/shared/api/http'
import type { KnowledgeNote, StudyRecord } from '@/shared/types/entities'
import type { PageResponse } from '@/shared/types/http'

export interface StudyRecordQuery {
  page?: number
  pageSize?: number
  from?: string
  to?: string
  noteId?: string
  stageId?: string
  keyword?: string
  tag?: string
}

export interface StudyRecordPayload {
  title?: string | null
  content: string
  understanding?: string | null
  questions?: string | null
  studyDate: string
  noteId?: string | null
  stageId?: string | null
  tagNames?: string[]
}

/** 学习记录表单初值（noteId/stageId 空字符串表示未选择） */
export interface StudyFormValue {
  id?: string
  title: string
  content: string
  understanding: string
  questions: string
  studyDate: string
  noteId: string
  stageId: string
  tagNames: string[]
}

export const studyApi = {
  list: (query: StudyRecordQuery) => get<PageResponse<StudyRecord>>('/study-records', query),
  get: (id: string) => get<StudyRecord>(`/study-records/${id}`),
  create: (payload: StudyRecordPayload) => post<{ id: string }>('/study-records', payload),
  update: (id: string, payload: StudyRecordPayload) => put<StudyRecord>(`/study-records/${id}`, payload),
  remove: (id: string) => del<null>(`/study-records/${id}`),
}

/** 关联笔记选项：取知识笔记第一页（供学习记录下拉使用） */
export function fetchNoteOptions(pageSize = 100): Promise<KnowledgeNote[]> {
  return get<PageResponse<KnowledgeNote>>('/knowledge/notes', { page: 1, pageSize }).then((r) => r.records)
}