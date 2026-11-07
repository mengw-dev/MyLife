/**
 * 数据管理 API（对齐 docs/02）：普通导出、AI 资料包、完整备份导出/校验/恢复。
 * 真实后端部分接口返回二进制文件（File/Blob），用 responseType: 'blob' 接收；
 * Mock 环境无真实文件，返回 {fileName, sizeBytes, content} 元信息由前端转 Blob 下载。
 */
import { post } from '@/shared/api/http'

const MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export type ExportFormat = 'markdown' | 'json' | 'zip'

export interface ExportPayload {
  format: ExportFormat
  include: string[]
  from?: string
  to?: string
}

/** Mock 环境下返回的可下载文件元信息（content 为文本内容） */
export interface DownloadFile {
  fileName: string
  sizeBytes: number
  content: string
}

export interface ValidateResult {
  valid: boolean
  message: string
}

export interface DownloadMeta {
  fileName: string
  sizeBytes: number
}

const contentTypeOf: Record<ExportFormat, string> = {
  markdown: 'text/markdown',
  json: 'application/json',
  zip: 'application/zip',
}

/** 触发浏览器下载（真实 blob 与 Mock 转出的 blob 共用） */
function triggerDownload(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export const dataApi = {
  /** 普通导出 */
  async exportFile(payload: ExportPayload): Promise<DownloadMeta> {
    if (MOCK) {
      const data = await post<DownloadFile>('/exports', payload)
      triggerDownload(new Blob([data.content], { type: contentTypeOf[payload.format] }), data.fileName)
      return { fileName: data.fileName, sizeBytes: data.sizeBytes }
    }
    const blob = await post<Blob>('/exports', payload, { responseType: 'blob' })
    const fileName = `myLife-export.${payload.format === 'markdown' ? 'md' : payload.format}`
    triggerDownload(blob, fileName)
    return { fileName, sizeBytes: blob.size }
  },

  /** AI 分析资料包 */
  async buildAiPackage(): Promise<DownloadMeta> {
    if (MOCK) {
      const data = await post<DownloadFile>('/exports/ai-package')
      triggerDownload(new Blob([data.content], { type: 'application/zip' }), data.fileName)
      return { fileName: data.fileName, sizeBytes: data.sizeBytes }
    }
    const blob = await post<Blob>('/exports/ai-package', undefined, { responseType: 'blob' })
    const fileName = 'myLife-ai-package.zip'
    triggerDownload(blob, fileName)
    return { fileName, sizeBytes: blob.size }
  },

  /** 完整备份导出 */
  async exportBackup(): Promise<DownloadMeta> {
    if (MOCK) {
      const data = await post<DownloadFile>('/backups/export')
      triggerDownload(new Blob([data.content], { type: 'application/zip' }), data.fileName)
      return { fileName: data.fileName, sizeBytes: data.sizeBytes }
    }
    const blob = await post<Blob>('/backups/export', undefined, { responseType: 'blob' })
    const fileName = 'MyLife-Backup.zip'
    triggerDownload(blob, fileName)
    return { fileName, sizeBytes: blob.size }
  },

  /** 备份文件校验：Mock 提交元信息，真实模式提交 multipart */
  validateBackup(file: File): Promise<ValidateResult> {
    if (MOCK) {
      return post<ValidateResult>('/backups/validate', { fileName: file.name, sizeBytes: file.size })
    }
    const form = new FormData()
    form.append('file', file)
    return post<ValidateResult>('/backups/validate', form)
  },

  /** 恢复备份 */
  restoreBackup(file: File): Promise<null> {
    if (MOCK) {
      return post<null>('/backups/restore', { fileName: file.name, sizeBytes: file.size })
    }
    const form = new FormData()
    form.append('file', file)
    return post<null>('/backups/restore', form)
  },
}