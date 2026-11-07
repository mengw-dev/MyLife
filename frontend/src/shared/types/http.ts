/**
 * 统一响应协议类型（对齐后端 docs/02 §1.5，code=0 为成功）
 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** 分页响应（page/pageSize/total/pages + records） */
export interface PageResponse<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
  pages: number
}