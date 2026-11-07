/**
 * 日记接口 Mock（对齐 docs/02 §4）：分页查询 + 增删改查。
 */
import type { InternalAxiosRequestConfig } from 'axios'
import { registerMock, ok, paginate, mockFail } from '@/shared/api/mock/registry'
import { mockDb, nextId } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { Diary } from '@/shared/types/entities'

interface DiaryBody {
  title?: string
  content?: string
  recordDate?: string
  stageId?: string | null
  tagNames?: string[]
  mediaIds?: string[]
}

function notFound(cfg: InternalAxiosRequestConfig) {
  return mockFail(cfg, 404, 40401, '日记不存在')
}

export function registerDiaryMock(): void {
  registerMock('GET', '/diaries', (_p, _b, q, cfg) => {
    requireMockUser(cfg)
    const kw = (q.keyword ?? '').toLowerCase()
    let list = mockDb.diaries.filter((d) => {
      const hit = !kw || `${d.title} ${d.content}`.toLowerCase().includes(kw)
      return hit
        && (!q.from || d.recordDate >= q.from)
        && (!q.to || d.recordDate <= q.to)
        && (!q.stageId || d.stageId === q.stageId)
        && (!q.tag || d.tagNames.includes(q.tag))
    })
    list = [...list].sort((a, b) => b.recordDate.localeCompare(a.recordDate))
    return ok(paginate(list, Number(q.page ?? 1), Number(q.pageSize ?? 20)))
  })

  registerMock('POST', '/diaries', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const b = (body ?? {}) as DiaryBody
    const now = new Date().toISOString()
    const diary: Diary = {
      id: nextId(),
      title: b.title ?? '',
      content: b.content ?? '',
      recordDate: b.recordDate ?? '',
      stageId: b.stageId ?? null,
      tagNames: b.tagNames ?? [],
      mediaIds: b.mediaIds ?? [],
      createdAt: now,
      updatedAt: now,
    }
    mockDb.diaries.push(diary)
    return ok({ id: diary.id }, 201)
  })

  registerMock('GET', '/diaries/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const diary = mockDb.diaries.find((d) => d.id === p.id)
    if (!diary) throw notFound(cfg)
    return ok(diary)
  })

  registerMock('PUT', '/diaries/:id', (p, body, _q, cfg) => {
    requireMockUser(cfg)
    const diary = mockDb.diaries.find((d) => d.id === p.id)
    if (!diary) throw notFound(cfg)
    const b = (body ?? {}) as DiaryBody
    diary.title = b.title ?? diary.title
    diary.content = b.content ?? diary.content
    diary.recordDate = b.recordDate ?? diary.recordDate
    diary.stageId = b.stageId ?? null
    diary.tagNames = b.tagNames ?? []
    diary.mediaIds = b.mediaIds ?? []
    diary.updatedAt = new Date().toISOString()
    return ok(diary)
  })

  registerMock('DELETE', '/diaries/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const diary = mockDb.diaries.find((d) => d.id === p.id)
    if (!diary) throw notFound(cfg)
    mockDb.diaries = mockDb.diaries.filter((d) => d.id !== p.id)
    return ok(null)
  })
}