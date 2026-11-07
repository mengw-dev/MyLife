/**
 * 搜索 API 与类型（对齐 docs/02）：跨日记/笔记/学习/事件检索。
 */
import { get } from '@/shared/api/http'
import type { PageResponse } from '@/shared/types/http'

export type SearchType = 'diary' | 'note' | 'study' | 'event'

export interface SearchItem {
  type: SearchType
  id: string
  title: string
  date: string
  snippet: string
}

export interface SearchParams {
  q: string
  types?: string
  page?: number
  pageSize?: number
}

export function search(params: SearchParams): Promise<PageResponse<SearchItem>> {
  return get<PageResponse<SearchItem>>('/search', { ...params })
}