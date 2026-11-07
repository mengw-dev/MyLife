/**
 * 数据管理接口 Mock（对齐 docs/02）：导出 / AI 资料包 / 备份导出校验恢复，均需登录。
 */
import { registerMock, ok } from '@/shared/api/mock/registry'
import { mockDb } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { ExportPayload } from '@/modules/data-management/api/data.api'

const textSize = (s: string) => new TextEncoder().encode(s).length

/** 依据 include 项挑选对应集合 */
function serialize(include: string[]): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  if (include.includes('diary')) out.diaries = mockDb.diaries
  if (include.includes('note')) out.notes = mockDb.notes
  if (include.includes('study')) out.studyRecords = mockDb.records
  if (include.includes('event')) out.events = mockDb.events
  if (include.includes('stage')) out.stages = mockDb.stages
  return out
}

/** markdown 简单拼接（不追求真实渲染，仅作 Mock 占位导出） */
function toMarkdown(sel: Record<string, unknown>): string {
  const lines: string[] = ['# MyLife 数据导出', '']
  Object.entries(sel).forEach(([name, arr]) => {
    lines.push(`## ${name}`, '')
    ;(arr as unknown[]).forEach((item) => lines.push(JSON.stringify(item, null, 2), ''))
  })
  return lines.join('\n')
}

/** 全量数据快照（不含用户口令） */
function snapshot(): Record<string, unknown> {
  return {
    users: mockDb.users,
    stages: mockDb.stages,
    yearProfiles: mockDb.yearProfiles,
    diaries: mockDb.diaries,
    categories: mockDb.categories,
    notes: mockDb.notes,
    records: mockDb.records,
    events: mockDb.events,
    medias: mockDb.medias,
  }
}

export function registerDataMock(): void {
  registerMock('POST', '/exports', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const { format = 'markdown', include = [] } = (body ?? {}) as ExportPayload
    const sel = serialize(include)
    if (format === 'json') {
      const content = JSON.stringify(sel, null, 2)
      return ok({ fileName: 'myLife-export.json', sizeBytes: textSize(content), content })
    }
    if (format === 'zip') {
      return ok({ fileName: 'myLife-export.zip', sizeBytes: 5120, content: '' })
    }
    const content = toMarkdown(sel)
    return ok({ fileName: 'myLife-export.md', sizeBytes: textSize(content), content })
  })

  registerMock('POST', '/exports/ai-package', (_p, _b, _q, cfg) => {
    requireMockUser(cfg)
    return ok({ fileName: 'myLife-ai-package.zip', sizeBytes: 2048, content: '' })
  })

  registerMock('POST', '/backups/export', (_p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const content = JSON.stringify(snapshot(), null, 2)
    return ok({ fileName: 'MyLife-Backup.zip', sizeBytes: textSize(content), content })
  })

  registerMock('POST', '/backups/validate', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const { fileName = '' } = (body ?? {}) as { fileName?: string }
    const valid = /\.(zip|json)$/i.test(fileName)
    return ok(valid ? { valid: true, message: '备份文件校验通过' } : { valid: false, message: '仅支持 .zip 或 .json 格式的备份文件' })
  })

  registerMock('POST', '/backups/restore', (_p, _b, _q, cfg) => {
    requireMockUser(cfg)
    return ok(null)
  })
}