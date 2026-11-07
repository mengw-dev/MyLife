/**
 * 相册 API（对齐 docs/02）：列表/详情/删除/上传。
 * 上传契约差异：真实后端要求 multipart/form-data（字段 file）；
 * Mock 环境无真实文件，退化为提交 JSON 元信息（originalName/mimeType/sizeBytes/description）。
 */
import { get, post, del } from '@/shared/api/http'
import type { MediaFile } from '@/shared/types/entities'
import type { PageResponse } from '@/shared/types/http'
import { mediaUrl } from '@/shared/utils/media'

const MOCK = import.meta.env.VITE_USE_MOCK === 'true'
const BASE = '/media'

export const mediaApi = {
  list: (page: number, pageSize: number) =>
    get<PageResponse<MediaFile>>(BASE, { page, pageSize }),

  detail: (id: string) => get<MediaFile>(`${BASE}/${id}`),

  remove: (id: string) => del<null>(`${BASE}/${id}`),

  /** 上传：Mock 提交 JSON 元信息，真实模式提交 FormData（file 字段，可选 description） */
  upload: (file: File, description?: string) => {
    if (MOCK) {
      return post<MediaFile>(BASE, {
        originalName: file.name,
        mimeType: file.type || 'application/octet-stream',
        sizeBytes: file.size,
        description: description || null,
      })
    }
    const form = new FormData()
    form.append('file', file)
    if (description) form.append('description', description)
    return post<MediaFile>(BASE, form)
  },

  /** 图片展示地址：Mock 为占位图，真实为鉴权内容接口 */
  url: mediaUrl,
}