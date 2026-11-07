/**
 * 重要事件 API 与请求/响应类型（对齐 docs/02 §8）。
 */
import { get, post, put, del } from '@/shared/api/http'
import type { ImportantEvent } from '@/shared/types/entities'
import type { PageResponse } from '@/shared/types/http'

export interface EventQuery {
  page?: number
  pageSize?: number
  from?: string
  to?: string
  stageId?: string
  tag?: string
}

export interface EventPayload {
  title: string
  eventDate: string
  description?: string | null
  stageId?: string | null
  diaryId?: string | null
  mediaIds?: string[]
  tagNames?: string[]
}

/** 重要事件表单初值（stageId/diaryId 空字符串表示未选择） */
export interface EventFormValue {
  id?: string
  title: string
  eventDate: string
  description: string
  stageId: string
  diaryId: string
  tagNames: string[]
  mediaIds: string[]
}

export const eventApi = {
  list: (query: EventQuery) => get<PageResponse<ImportantEvent>>('/events', query),
  get: (id: string) => get<ImportantEvent>(`/events/${id}`),
  create: (payload: EventPayload) => post<{ id: string }>('/events', payload),
  update: (id: string, payload: EventPayload) => put<ImportantEvent>(`/events/${id}`, payload),
  remove: (id: string) => del<null>(`/events/${id}`),
}