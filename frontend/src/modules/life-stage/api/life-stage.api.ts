/**
 * 人生阶段 API 与请求/响应类型（对齐 docs/02 §3）。
 */
import { get, post, put, del } from '@/shared/api/http'
import type { LifeStage } from '@/shared/types/entities'

export interface LifeStagePayload {
  name: string
  startDate: string
  endDate?: string | null
  description?: string | null
  coverMediaId?: string | null
  sortOrder?: number
}

export interface ReorderItem {
  id: string
  sortOrder: number
}

export const lifeStageApi = {
  list: () => get<LifeStage[]>('/life-stages'),
  get: (id: string) => get<LifeStage>(`/life-stages/${id}`),
  create: (payload: LifeStagePayload) => post<{ id: string }>('/life-stages', payload),
  update: (id: string, payload: LifeStagePayload) => put<LifeStage>(`/life-stages/${id}`, payload),
  remove: (id: string) => del<null>(`/life-stages/${id}`),
  reorder: (items: ReorderItem[]) => put<null>('/life-stages/order', { items }),
  setMedia: (id: string, mediaIds: string[]) => put<null>(`/life-stages/${id}/media`, { mediaIds }),
}