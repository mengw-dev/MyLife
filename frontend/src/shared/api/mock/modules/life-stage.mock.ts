/**
 * 人生阶段接口 Mock（对齐 docs/02 §3）：列表 + 增删改查 + 排序 + 照片。
 */
import type { InternalAxiosRequestConfig } from 'axios'
import { registerMock, ok, mockFail } from '@/shared/api/mock/registry'
import { mockDb, nextId } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { LifeStage } from '@/shared/types/entities'

interface StageBody {
  name?: string
  startDate?: string
  endDate?: string | null
  description?: string | null
  coverMediaId?: string | null
  sortOrder?: number
}

function notFound(cfg: InternalAxiosRequestConfig) {
  return mockFail(cfg, 404, 40401, '阶段不存在')
}

export function registerLifeStageMock(): void {
  // 注意：/life-stages/order 必须注册在 /life-stages/:id 之前，避免被 :id 段拦截
  registerMock('PUT', '/life-stages/order', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const items = ((body as { items?: { id: string; sortOrder: number }[] })?.items) ?? []
    for (const it of items) {
      const stage = mockDb.stages.find((s) => s.id === it.id)
      if (stage) stage.sortOrder = it.sortOrder
    }
    return ok(null)
  })

  registerMock('GET', '/life-stages', (_p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const list = [...mockDb.stages].sort((a, b) => a.sortOrder - b.sortOrder)
    return ok(list)
  })

  registerMock('POST', '/life-stages', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const b = (body ?? {}) as StageBody
    const now = new Date().toISOString()
    const maxOrder = mockDb.stages.reduce((m, s) => Math.max(m, s.sortOrder), 0)
    const stage: LifeStage = {
      id: nextId(),
      name: b.name ?? '',
      startDate: b.startDate ?? '',
      endDate: b.endDate ?? null,
      description: b.description ?? null,
      coverMediaId: b.coverMediaId ?? null,
      sortOrder: b.sortOrder ?? maxOrder + 1,
      mediaIds: [],
      createdAt: now,
      updatedAt: now,
    }
    mockDb.stages.push(stage)
    return ok({ id: stage.id }, 201)
  })

  registerMock('GET', '/life-stages/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const stage = mockDb.stages.find((s) => s.id === p.id)
    if (!stage) throw notFound(cfg)
    return ok(stage)
  })

  registerMock('PUT', '/life-stages/:id', (p, body, _q, cfg) => {
    requireMockUser(cfg)
    const stage = mockDb.stages.find((s) => s.id === p.id)
    if (!stage) throw notFound(cfg)
    const b = (body ?? {}) as StageBody
    stage.name = b.name ?? stage.name
    stage.startDate = b.startDate ?? stage.startDate
    stage.endDate = b.endDate ?? null
    stage.description = b.description ?? null
    stage.coverMediaId = b.coverMediaId ?? null
    if (typeof b.sortOrder === 'number') stage.sortOrder = b.sortOrder
    stage.updatedAt = new Date().toISOString()
    return ok(stage)
  })

  registerMock('PUT', '/life-stages/:id/media', (p, body, _q, cfg) => {
    requireMockUser(cfg)
    const stage = mockDb.stages.find((s) => s.id === p.id)
    if (!stage) throw notFound(cfg)
    stage.mediaIds = ((body as { mediaIds?: string[] })?.mediaIds) ?? []
    stage.updatedAt = new Date().toISOString()
    return ok(stage)
  })

  registerMock('DELETE', '/life-stages/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const stage = mockDb.stages.find((s) => s.id === p.id)
    if (!stage) throw notFound(cfg)
    mockDb.stages = mockDb.stages.filter((s) => s.id !== p.id)
    // 删除规则：关联的日记/学习/事件保留，stageId 置空
    mockDb.diaries.forEach((d) => { if (d.stageId === p.id) d.stageId = null })
    mockDb.records.forEach((r) => { if (r.stageId === p.id) r.stageId = null })
    mockDb.events.forEach((e) => { if (e.stageId === p.id) e.stageId = null })
    return ok(null)
  })
}