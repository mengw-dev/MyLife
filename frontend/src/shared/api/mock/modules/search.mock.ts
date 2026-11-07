/**
 * 搜索接口 Mock（对齐 docs/02）：聚合日记/笔记/学习/事件，按命中字段截取片段。
 */
import { registerMock, okPage, paginate } from '@/shared/api/mock/registry'
import { mockDb } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { SearchItem, SearchType } from '@/modules/search/api/search.api'

const CONTEXT = 20

/** 在正文中定位关键词，截取前后各 20 字并补省略号 */
function makeSnippet(text: string, q: string): string | null {
  const idx = text.toLowerCase().indexOf(q.toLowerCase())
  if (idx < 0) return null
  const start = Math.max(0, idx - CONTEXT)
  const end = Math.min(text.length, idx + q.length + CONTEXT)
  return `${start > 0 ? '…' : ''}${text.slice(start, end)}${end < text.length ? '…' : ''}`
}

export function registerSearchMock(): void {
  registerMock('GET', '/search', (_p, _b, q, cfg) => {
    requireMockUser(cfg)
    const keyword = (q.q ?? '').trim()
    const page = Number(q.page ?? 1)
    const pageSize = Number(q.pageSize ?? 10)
    if (!keyword) {
      return okPage([], 1, Math.max(1, pageSize), 0)
    }

    const wanted = (q.types ?? '').split(',').filter(Boolean)
    const want = (t: SearchType) => wanted.length === 0 || wanted.includes(t)

    const items: SearchItem[] = []
    const collect = (type: SearchType, id: string, title: string, date: string, content: string) => {
      if (!want(type)) return
      const snippet = makeSnippet(title, keyword) ?? makeSnippet(content, keyword)
      if (!snippet) return
      items.push({ type, id, title, date, snippet })
    }

    mockDb.diaries.forEach((d) => collect('diary', d.id, d.title, d.recordDate, d.content))
    mockDb.notes.forEach((n) => collect('note', n.id, n.title, n.createdAt.slice(0, 10), n.contentMarkdown))
    mockDb.records.forEach((r) => collect('study', r.id, r.title ?? '', r.studyDate, r.content))
    mockDb.events.forEach((e) => collect('event', e.id, e.title, e.eventDate, e.description ?? ''))

    items.sort((a, b) => b.date.localeCompare(a.date))
    const result = paginate(items, page, pageSize)
    return okPage(result.records, result.page, result.pageSize, result.total)
  })
}