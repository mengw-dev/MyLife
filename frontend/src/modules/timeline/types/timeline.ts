/**
 * 时间线统一数据结构（DEV-NOTES §4）：所有展示组件复用本类型，不做二次聚合。
 */
import type { Diary, ImportantEvent, StudyRecord } from '@/shared/types/entities'

/** 年份总览节点（GET /timeline/years） */
export interface TimelineYearSummary {
  year: number
  title: string | null
  summary: string | null
  coverMediaId: string | null
  diaryCount: number
  studyCount: number
  eventCount: number
  hasRecords: boolean
}

/** 年度详情中的单月统计（GET /timeline/years/{year}） */
export interface YearMonthStat {
  month: number
  diaryCount: number
  studyCount: number
  eventCount: number
  hasRecords: boolean
}

/** 年度详情（含全年重要事件） */
export interface TimelineYearDetail {
  year: number
  title: string | null
  summary: string | null
  coverMediaId: string | null
  months: YearMonthStat[]
  events: ImportantEvent[]
}

/** 月份日历中的单日统计（GET /timeline/months/{year}/{month}） */
export interface TimelineDayStat {
  date: string
  diaryCount: number
  studyCount: number
  eventCount: number
  hasRecords: boolean
}

/** 日期详情（GET /timeline/days/{date}） */
export interface TimelineDay {
  date: string
  diaries: Diary[]
  studyRecords: StudyRecord[]
  events: ImportantEvent[]
}

/** 年份筛选参数（时间线总览） */
export interface YearsQuery {
  fromYear?: number
  toYear?: number
}

/** 时间线层级（用于面包屑与转场方向判断） */
export type TimelineDepth = 'overview' | 'year' | 'month' | 'day'