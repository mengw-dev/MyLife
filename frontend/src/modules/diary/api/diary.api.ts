/**
 * 日记 API 与请求/响应类型（对齐 docs/02 §4）。
 */
import { get, post, put, del } from '@/shared/api/http'
import type { Diary, MediaFile } from '@/shared/types/entities'
import type { PageResponse } from '@/shared/types/http'

export interface DiaryQuery {
  page?: number
  pageSize?: number
  keyword?: string
  from?: string
  to?: string
  stageId?: string
  tag?: string
}

/** 日记写请求体：PUT 全量替换语义 */
export interface DiaryPayload {
  title: string
  content: string
  recordDate: string
  stageId?: string | null
  tagNames?: string[]
  mediaIds?: string[]
}

/** 日记表单初值（stageId 空字符串表示未选择） */
export interface DiaryFormValue {
  id?: string
  title: string
  content: string
  recordDate: string
  stageId: string
  tagNames: string[]
  mediaIds: string[]
}

export const diaryApi = {
  list: (query: DiaryQuery) => get<PageResponse<Diary>>('/diaries', query),
  get: (id: string) => get<Diary>(`/diaries/${id}`),
  create: (payload: DiaryPayload) => post<{ id: string }>('/diaries', payload),
  update: (id: string, payload: DiaryPayload) => put<Diary>(`/diaries/${id}`, payload),
  remove: (id: string) => del<null>(`/diaries/${id}`),
}

/** 照片选项：取媒体库第一页（供日记/事件的照片多选下拉使用） */
export function fetchMediaFirstPage(pageSize = 24): Promise<MediaFile[]> {
  return get<PageResponse<MediaFile>>('/media', { page: 1, pageSize }).then((r) => r.records)
}