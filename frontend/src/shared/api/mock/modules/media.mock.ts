/**
 * 相册接口 Mock（对齐 docs/02）：列表/详情/创建/删除。
 * 删除前校验引用关系（日记/笔记/事件 mediaIds、阶段/年份封面 coverMediaId），被引用时返回 409。
 */
import { registerMock, ok, okPage, paginate, mockFail } from '@/shared/api/mock/registry'
import { mockDb, nextId } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { MediaFile } from '@/shared/types/entities'

export function registerMediaMock(): void {
  registerMock('GET', '/media', (_p, _b, q, cfg) => {
    requireMockUser(cfg)
    const page = Number(q.page ?? 1)
    const pageSize = Number(q.pageSize ?? 20)
    const sorted = [...mockDb.medias].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    const result = paginate(sorted, page, pageSize)
    return okPage(result.records, result.page, result.pageSize, result.total)
  })

  registerMock('GET', '/media/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const media = mockDb.medias.find((m) => m.id === p.id)
    if (!media) throw mockFail(cfg, 404, 40401, '照片不存在')
    return ok(media)
  })

  registerMock('POST', '/media', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const { originalName, mimeType, sizeBytes, description } = (body ?? {}) as {
      originalName?: string
      mimeType?: string
      sizeBytes?: number
      description?: string | null
    }
    if (!originalName) {
      return { status: 400, body: { code: 40001, message: '缺少文件名', data: null } }
    }
    const media: MediaFile = {
      id: nextId(),
      originalName,
      mimeType: mimeType ?? 'application/octet-stream',
      sizeBytes: Number(sizeBytes ?? 0),
      description: description ?? null,
      width: null,
      height: null,
      createdAt: new Date().toISOString(),
    }
    mockDb.medias.push(media)
    return ok(media, 201)
  })

  registerMock('DELETE', '/media/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const id = p.id
    if (!mockDb.medias.some((m) => m.id === id)) {
      throw mockFail(cfg, 404, 40401, '照片不存在')
    }
    const referenced =
      mockDb.diaries.some((d) => d.mediaIds.includes(id)) ||
      mockDb.notes.some((n) => n.mediaIds.includes(id)) ||
      mockDb.events.some((e) => e.mediaIds.includes(id)) ||
      mockDb.stages.some((s) => s.coverMediaId === id) ||
      mockDb.yearProfiles.some((y) => y.coverMediaId === id)
    if (referenced) {
      throw mockFail(cfg, 409, 40901, '照片仍被引用，无法删除')
    }
    mockDb.medias = mockDb.medias.filter((m) => m.id !== id)
    return ok(null)
  })
}