/**
 * 知识库 API 与请求/响应类型（对齐 docs/02 知识库契约）。
 */
import { get, post, put, del } from '@/shared/api/http'
import type { KnowledgeCategory, KnowledgeNote, StudyRecord } from '@/shared/types/entities'
import type { PageResponse } from '@/shared/types/http'

/** 分类新建/修改请求体 */
export interface CategoryPayload {
  name: string
  parentId?: string | null
  sortOrder?: number
}

/** 笔记新建/修改请求体 */
export interface NotePayload {
  title: string
  contentMarkdown: string
  categoryId: string
  tagNames: string[]
  mediaIds?: string[]
}

/** 笔记分页查询参数 */
export interface NoteQuery {
  page: number
  pageSize: number
  keyword?: string
  categoryId?: string
  tag?: string
}

const BASE = '/knowledge'

export const knowledgeApi = {
  /** 分类树 */
  getCategoryTree: () => get<KnowledgeCategory[]>(`${BASE}/categories`),
  /** 新建分类 → {id} */
  createCategory: (payload: CategoryPayload) => post<{ id: string }>(`${BASE}/categories`, payload),
  /** 修改分类 */
  updateCategory: (id: string, payload: CategoryPayload) => put<null>(`${BASE}/categories/${id}`, payload),
  /** 删除分类 */
  deleteCategory: (id: string) => del<null>(`${BASE}/categories/${id}`),

  /** 笔记分页列表 */
  listNotes: (params: NoteQuery) => get<PageResponse<KnowledgeNote>>(`${BASE}/notes`, { ...params }),
  /** 新建笔记 → {id} */
  createNote: (payload: NotePayload) => post<{ id: string }>(`${BASE}/notes`, payload),
  /** 笔记详情 */
  getNote: (id: string) => get<KnowledgeNote>(`${BASE}/notes/${id}`),
  /** 修改笔记 */
  updateNote: (id: string, payload: NotePayload) => put<null>(`${BASE}/notes/${id}`, payload),
  /** 删除笔记 */
  deleteNote: (id: string) => del<null>(`${BASE}/notes/${id}`),
  /** 笔记关联的学习记录 */
  listStudyRecords: (noteId: string) => get<StudyRecord[]>(`${BASE}/notes/${noteId}/study-records`),
}