/**
 * 时间线数据状态：年份总览 + 年月日详情（统一聚合数据，供主时间线与预览复用）。
 */
import { defineStore } from 'pinia'
import { fetchDayDetail, fetchMonthDetail, fetchTimelineYears, fetchYearDetail } from '@/modules/timeline/api/timeline.api'
import type { TimelineDay, TimelineDayStat, TimelineYearDetail, TimelineYearSummary } from '@/modules/timeline/types/timeline'

interface TimelineState {
  years: TimelineYearSummary[]
  yearsLoading: boolean
  yearDetail: TimelineYearDetail | null
  yearLoading: boolean
  monthDays: TimelineDayStat[]
  monthLoading: boolean
  monthKey: string
  day: TimelineDay | null
  dayLoading: boolean
}

export const useTimelineStore = defineStore('timeline', {
  state: (): TimelineState => ({
    years: [],
    yearsLoading: false,
    yearDetail: null,
    yearLoading: false,
    monthDays: [],
    monthLoading: false,
    monthKey: '',
    day: null,
    dayLoading: false,
  }),

  actions: {
    async loadYears(fromYear?: number, toYear?: number) {
      this.yearsLoading = true
      try {
        this.years = await fetchTimelineYears(fromYear, toYear)
      } finally {
        this.yearsLoading = false
      }
    },

    /** 若目标年份未加载或缓存过期则重载（expired=true 强制刷新，用于保存后同步） */
    async loadYear(year: number, expired = false) {
      if (!expired && this.yearDetail?.year === year) return
      this.yearLoading = true
      try {
        this.yearDetail = await fetchYearDetail(year)
      } finally {
        this.yearLoading = false
      }
    },

    async loadMonth(year: number, month: number, expired = false) {
      const key = `${year}-${String(month).padStart(2, '0')}`
      if (!expired && this.monthKey === key && this.monthDays.length) return
      this.monthLoading = true
      try {
        this.monthDays = await fetchMonthDetail(year, month)
        this.monthKey = key
      } finally {
        this.monthLoading = false
      }
    },

    async loadDay(date: string, expired = false) {
      if (!expired && this.day?.date === date) return
      this.dayLoading = true
      try {
        this.day = await fetchDayDetail(date)
      } finally {
        this.dayLoading = false
      }
    },

    /** 保存成功后同步刷新当前已加载层级 */
    async refreshCurrent() {
      if (this.day) await this.loadDay(this.day.date, true)
      if (this.yearDetail) await this.loadYear(this.yearDetail.year, true)
      await this.loadYears(undefined, undefined)
    },
  },
})