/**
 * 时间线 API：真实后端就绪后与 Mock 行为一致（均为 docs/02 §11 契约）。
 */
import { get } from '@/shared/api/http'
import type { TimelineDay, TimelineDayStat, TimelineYearDetail, TimelineYearSummary } from '../types/timeline'

const BASE = '/timeline'

export function fetchTimelineYears(fromYear?: number, toYear?: number): Promise<TimelineYearSummary[]> {
  return get<TimelineYearSummary[]>(`${BASE}/years`, { fromYear, toYear })
}

export function fetchYearDetail(year: number): Promise<TimelineYearDetail> {
  return get<TimelineYearDetail>(`${BASE}/years/${year}`)
}

export function fetchMonthDetail(year: number, month: number): Promise<TimelineDayStat[]> {
  return get<TimelineDayStat[]>(`${BASE}/months/${year}/${month}`)
}

export function fetchDayDetail(date: string): Promise<TimelineDay> {
  return get<TimelineDay>(`${BASE}/days/${date}`)
}