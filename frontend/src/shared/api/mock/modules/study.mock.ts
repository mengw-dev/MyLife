/**
 * 学习记录接口 Mock（对齐 docs/02 §7）：分页查询 + 增删改查。
 */
import type { InternalAxiosRequestConfig } from 'axios'
import { registerMock, ok, paginate, mockFail } from '@/shared/api/mock/registry'
import { mockDb, nextId } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { StudyRecord } from '@/shared/types/entities'

interface StudyBody {
  title?: string | null
  content?: string
  understanding?: string | null
  questions?: string | null
  studyDate?: string
  noteId?: string | null
  stageId?: string | null
  tagNames?: string[]
}

function notFound(cfg: InternalAxiosRequestConfig) {
  return mockFail(cfg, 404, 40401, '学习记录不存在')
}

function noteSnapshot(noteId?: string | null): string | null {
  if (!noteId) return null
  return mockDb.notes.find((n) => n.id === noteId)?.title ?? null
}

export function registerStudyMock(): void {
  registerMock('GET', '/study-records', (_p, _b, q, cfg) => {
    requireMockUser(cfg)
    const kw = (q.keyword ?? '').toLowerCase()
    let list = mockDb.records.filter((r) => {
      const hit = !kw || `${r.title ?? ''} ${r.content} ${r.understanding ?? ''} ${r.questions ?? ''}`.toLowerCase().includes(kw)
      return hit
        && (!q.from || r.studyDate >= q.from)
        && (!q.to || r.studyDate <= q.to)
        && (!q.noteId || r.noteId === q.noteId)
        && (!q.stageId || r.stageId === q.stageId)
        && (!q.tag || r.tagNames.includes(q.tag))
    })
    list = [...list].sort((a, b) => b.studyDate.localeCompare(a.studyDate))
    return ok(paginate(list, Number(q.page ?? 1), Number(q.pageSize ?? 20)))
  })

  registerMock('POST', '/study-records', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const b = (body ?? {}) as StudyBody
    const now = new Date().toISOString()
    const record: StudyRecord = {
      id: nextId(),
      title: b.title ?? null,
      content: b.content ?? '',
      understanding: b.understanding ?? null,
      questions: b.questions ?? null,
      studyDate: b.studyDate ?? '',
      noteId: b.noteId ?? null,
      noteTitleSnapshot: noteSnapshot(b.noteId),
      stageId: b.stageId ?? null,
      tagNames: b.tagNames ?? [],
      createdAt: now,
      updatedAt: now,
    }
    mockDb.records.push(record)
    return ok({ id: record.id }, 201)
  })

  registerMock('GET', '/study-records/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const record = mockDb.records.find((r) => r.id === p.id)
    if (!record) throw notFound(cfg)
    return ok(record)
  })

  registerMock('PUT', '/study-records/:id', (p, body, _q, cfg) => {
    requireMockUser(cfg)
    const record = mockDb.records.find((r) => r.id === p.id)
    if (!record) throw notFound(cfg)
    const b = (body ?? {}) as StudyBody
    record.title = b.title ?? record.title
    record.content = b.content ?? record.content
    record.understanding = b.understanding ?? null
    record.questions = b.questions ?? null
    record.studyDate = b.studyDate ?? record.studyDate
    record.noteId = b.noteId ?? null
    record.noteTitleSnapshot = noteSnapshot(b.noteId)
    record.stageId = b.stageId ?? null
    record.tagNames = b.tagNames ?? []
    record.updatedAt = new Date().toISOString()
    return ok(record)
  })

  registerMock('DELETE', '/study-records/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const record = mockDb.records.find((r) => r.id === p.id)
    if (!record) throw notFound(cfg)
    mockDb.records = mockDb.records.filter((r) => r.id !== p.id)
    return ok(null)
  })
}