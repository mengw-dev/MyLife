/**
 * 时间线聚合数据 Mock（对齐 docs/02 §11，基于内存库实时聚合）
 */
import { registerMock, ok } from '@/shared/api/mock/registry'
import { mockDb } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { YearMonthStat, TimelineYearSummary, TimelineDayStat } from '@/modules/timeline/types/timeline'

const YEAR_KEYS = ['2021', '2022', '2023', '2024', '2025', '2026']

function countStats(): Map<string, Map<string, { diary: number; study: number; event: number }>> {
  const byDate = new Map<string, Map<string, { diary: number; study: number; event: number }>>()
  const bump = (date: string, kind: 'diary' | 'study' | 'event') => {
    if (!date) return
    const ym = date.slice(0, 7)
    if (!byDate.has(ym)) byDate.set(ym, new Map())
    const day = byDate.get(ym)!
    if (!day.has(date)) day.set(date, { diary: 0, study: 0, event: 0 })
    day.get(date)![kind] += 1
  }
  mockDb.diaries.forEach((d) => bump(d.recordDate, 'diary'))
  mockDb.records.forEach((r) => bump(r.studyDate, 'study'))
  mockDb.events.forEach((e) => bump(e.eventDate, 'event'))
  return byDate
}

export function registerTimelineMock(): void {
  registerMock('GET', '/timeline/years', (_p, _b, q, cfg) => {
    requireMockUser(cfg)
    const byDate = countStats()
    const from = Number(q.fromYear ?? 2021)
    const to = Number(q.toYear ?? 2026)
    const years: TimelineYearSummary[] = []
    for (let y = from; y <= to; y++) {
      const months = [...byDate.keys()].filter((k) => k.startsWith(String(y)))
      if (months.length === 0 && mockDb.yearProfiles.every((p) => p.year !== y)) {
        years.push({ year: y, title: null, summary: null, coverMediaId: null, diaryCount: 0, studyCount: 0, eventCount: 0, hasRecords: false })
        continue
      }
      let diary = 0, study = 0, event = 0
      months.forEach((m) => {
        byDate.get(m)!.forEach((v) => { diary += v.diary; study += v.study; event += v.event })
      })
      const profile = mockDb.yearProfiles.find((p) => p.year === y)
      years.push({
        year: y,
        title: profile?.title ?? null,
        summary: profile?.summary ?? null,
        coverMediaId: profile?.coverMediaId ?? null,
        diaryCount: diary,
        studyCount: study,
        eventCount: event,
        hasRecords: true,
      })
    }
    return ok(years)
  })

  registerMock('GET', '/timeline/years/:year', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const year = Number(p.year)
    const byDate = countStats()
    const months: YearMonthStat[] = []
    for (let m = 1; m <= 12; m++) {
      const key = `${year}-${String(m).padStart(2, '0')}`
      const days = byDate.get(key)
      const stat = { diary: 0, study: 0, event: 0 }
      days?.forEach((v) => { stat.diary += v.diary; stat.study += v.study; stat.event += v.event })
      months.push({ month: m, diaryCount: stat.diary, studyCount: stat.study, eventCount: stat.event, hasRecords: stat.diary + stat.study + stat.event > 0 })
    }
    const profile = mockDb.yearProfiles.find((p) => p.year === year)
    const events = mockDb.events.filter((e) => e.eventDate.startsWith(String(year)))
    return ok({
      year,
      title: profile?.title ?? null,
      summary: profile?.summary ?? null,
      coverMediaId: profile?.coverMediaId ?? null,
      months,
      events: [...events].sort((a, b) => b.eventDate.localeCompare(a.eventDate)),
    })
  })

  registerMock('GET', '/timeline/months/:year/:month', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const ym = `${p.year}-${String(Number(p.month)).padStart(2, '0')}`
    const days = countStats().get(ym)
    const result: TimelineDayStat[] = []
    days?.forEach((v, date) => {
      result.push({ date, diaryCount: v.diary, studyCount: v.study, eventCount: v.event, hasRecords: v.diary + v.study + v.event > 0 })
    })
    return ok(result.sort((a, b) => a.date.localeCompare(b.date)))
  })

  registerMock('GET', '/timeline/days/:date', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const date = p.date
    const diaries = mockDb.diaries.filter((d) => d.recordDate === date)
    const studyRecords = mockDb.records.filter((r) => r.studyDate === date)
    const events = mockDb.events.filter((e) => e.eventDate === date)
    return ok({ date, diaries, studyRecords, events })
  })
}

// 供其他视图使用的年份范围常量（与后端默认行为一致的演示范围）
export { YEAR_KEYS }