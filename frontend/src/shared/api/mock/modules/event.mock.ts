/**
 * 重要事件接口 Mock（对齐 docs/02 §8）：分页查询 + 增删改查。
 */
import type { InternalAxiosRequestConfig } from 'axios'
import { registerMock, ok, paginate, mockFail } from '@/shared/api/mock/registry'
import { mockDb, nextId } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { ImportantEvent } from '@/shared/types/entities'

interface EventBody {
  title?: string
  eventDate?: string
  description?: string | null
  stageId?: string | null
  diaryId?: string | null
  mediaIds?: string[]
  tagNames?: string[]
}

function notFound(cfg: InternalAxiosRequestConfig) {
  return mockFail(cfg, 404, 40401, '事件不存在')
}

export function registerEventMock(): void {
  registerMock('GET', '/events', (_p, _b, q, cfg) => {
    requireMockUser(cfg)
    let list = mockDb.events.filter((e) =>
      (!q.from || e.eventDate >= q.from)
      && (!q.to || e.eventDate <= q.to)
      && (!q.stageId || e.stageId === q.stageId)
      && (!q.tag || e.tagNames.includes(q.tag))
    )
    list = [...list].sort((a, b) => b.eventDate.localeCompare(a.eventDate))
    return ok(paginate(list, Number(q.page ?? 1), Number(q.pageSize ?? 20)))
  })

  registerMock('POST', '/events', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const b = (body ?? {}) as EventBody
    const now = new Date().toISOString()
    const event: ImportantEvent = {
      id: nextId(),
      title: b.title ?? '',
      eventDate: b.eventDate ?? '',
      description: b.description ?? null,
      stageId: b.stageId ?? null,
      diaryId: b.diaryId ?? null,
      mediaIds: b.mediaIds ?? [],
      tagNames: b.tagNames ?? [],
      createdAt: now,
      updatedAt: now,
    }
    mockDb.events.push(event)
    return ok({ id: event.id }, 201)
  })

  registerMock('GET', '/events/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const event = mockDb.events.find((e) => e.id === p.id)
    if (!event) throw notFound(cfg)
    return ok(event)
  })

  registerMock('PUT', '/events/:id', (p, body, _q, cfg) => {
    requireMockUser(cfg)
    const event = mockDb.events.find((e) => e.id === p.id)
    if (!event) throw notFound(cfg)
    const b = (body ?? {}) as EventBody
    event.title = b.title ?? event.title
    event.eventDate = b.eventDate ?? event.eventDate
    event.description = b.description ?? null
    event.stageId = b.stageId ?? null
    event.diaryId = b.diaryId ?? null
    event.mediaIds = b.mediaIds ?? []
    event.tagNames = b.tagNames ?? []
    event.updatedAt = new Date().toISOString()
    return ok(event)
  })

  registerMock('DELETE', '/events/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const event = mockDb.events.find((e) => e.id === p.id)
    if (!event) throw notFound(cfg)
    mockDb.events = mockDb.events.filter((e) => e.id !== p.id)
    return ok(null)
  })
}