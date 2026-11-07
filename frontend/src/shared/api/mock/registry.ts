/**
 * Mock 路由注册与匹配：模块在自己的 api/<领域>.mock.ts 中调用 registerMock 注册处理器。
 */
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { AxiosError as AxiosErr, AxiosHeaders } from 'axios'
import type { ApiResponse } from '@/shared/types/http'

/** 模拟响应体：status 为 HTTP 状态码，body 是标准 ApiResponse 结构 */
export interface MockResult {
  status: number
  body: ApiResponse<unknown>
}

export type MockHandler = (params: Record<string, string>, body: unknown, query: Record<string, string>, config: InternalAxiosRequestConfig) => MockResult | Promise<MockResult>

interface Route {
  method: string
  path: string
  segments: string[]
  handler: MockHandler
}

const routes: Route[] = []

/** 注册一条模拟路由，path 支持 /:id 风格参数 */
export function registerMock(method: string, path: string, handler: MockHandler): void {
  routes.push({ method: method.toUpperCase(), path, segments: path.split('/').filter(Boolean), handler })
}

interface Matched {
  handler: MockHandler
  params: Record<string, string>
}

/** 匹配 config 对应的模拟路由：找不到返回 null（由适配器回退 404） */
export function matchMock(config: InternalAxiosRequestConfig): Matched | null {
  const raw = (config.url ?? '').replace(/^\/api\/v1/, '')
  const [pathPart, queryPart] = raw.split('?')
  const path = pathPart.replace(/^\/+|\/+$/g, '')
  const segments = path.split('/').filter(Boolean)
  const query: Record<string, string> = {}
  if (queryPart) {
    for (const [k, v] of new URLSearchParams(queryPart)) query[k] = v
  }
  const method = (config.method ?? 'get').toUpperCase()

  for (const route of routes) {
    if (route.method !== method || route.segments.length !== segments.length) continue
    const params: Record<string, string> = {}
    let ok = true
    for (let i = 0; i < segments.length; i++) {
      const seg = route.segments[i]
      if (seg.startsWith(':')) params[seg.slice(1)] = decodeURIComponent(segments[i])
      else if (seg !== segments[i]) { ok = false; break }
    }
    if (!ok) continue
    return { handler: route.handler, params }
  }
  return null
}

/** 构造业务异常（带完整 AxiosError，可被统一拦截器处理） */
export function mockFail(config: InternalAxiosRequestConfig, status: number, code: number, message: string): AxiosError {
  const body: ApiResponse<unknown> = { code, message, data: null }
  return new AxiosErr(message, AxiosErr.ERR_BAD_RESPONSE, config, null, {
    data: body,
    status,
    statusText: String(status),
    headers: new AxiosHeaders(),
    config,
  })
}

/** 模拟未登录错误（401/40101，与后端 SecurityConfig 文案一致） */
export function mockUnauthorized(config: InternalAxiosRequestConfig): AxiosError {
  return mockFail(config, 401, 40101, '未登录或登录已过期')
}

/** 模拟业务成功响应 */
export function ok<T>(data: T, status = 200): MockResult {
  return { status, body: { code: 0, message: 'success', data } }
}

/** 模拟分页响应 */
export function okPage<T>(records: T[], page: number, pageSize: number, total: number): MockResult {
  return ok({ records, page, pageSize, total, pages: Math.ceil(total / pageSize) })
}

/** 多字段模糊检索 + 日期区间筛选的通用分页 */
export function paginate<T>(list: T[], page: number, pageSize: number): { records: T[]; page: number; pageSize: number; total: number } {
  const safePage = Math.max(1, page)
  const safeSize = Math.min(100, Math.max(1, pageSize))
  const start = (safePage - 1) * safeSize
  return { records: list.slice(start, start + safeSize), page: safePage, pageSize: safeSize, total: list.length }
}