/**
 * HTTP 客户端统一封装（DEV-NOTES §3）
 * - Session Cookie 自动携带，CSRF Token 自动附加（写请求）
 * - 响应统一解包 ApiResponse，code!=0 自动提示并 reject
 * - 401 统一跳登录（通过 onUnauthorized 回调解耦路由依赖）
 * - VITE_USE_MOCK=true 时切换到 Mock 适配器
 */
import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { mockAdapter } from './mock/adapter'
import type { ApiResponse } from '@/shared/types/http'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  withCredentials: true,
  timeout: 20000,
})

if (USE_MOCK) {
  http.defaults.adapter = mockAdapter
}

// ---- CSRF（真实后端专用；Mock 模式跳过） ----
let csrfToken: string | null = null
let csrfPending: Promise<string> | null = null

function readXsrfCookie(): string | null {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return m ? decodeURIComponent(m[1]) : null
}

async function fetchCsrfToken(): Promise<string> {
  const raw = axios.get(`${http.defaults.baseURL}/auth/csrf`, { withCredentials: true })
  const res = await raw
  csrfToken = (res.data as ApiResponse<{ token: string }>).data.token
  return csrfToken
}

async function ensureCsrfToken(): Promise<string | null> {
  const fromCookie = readXsrfCookie()
  if (fromCookie) return fromCookie
  if (csrfToken) return csrfToken
  if (!csrfPending) {
    csrfPending = fetchCsrfToken().finally(() => { csrfPending = null })
  }
  return csrfPending
}

http.interceptors.request.use(async (config) => {
  const method = (config.method ?? 'get').toUpperCase()
  const isWrite = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  if (isWrite && !USE_MOCK) {
    const token = await ensureCsrfToken()
    if (token) config.headers.set('X-XSRF-TOKEN', token)
  }
  return config
})

// ---- 响应统一处理 ----
let onUnauthorized: () => void = () => {}

export function setOnUnauthorized(handler: () => void): void {
  onUnauthorized = handler
}

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    // Mock 适配器返回的就是 ApiResponse；真实后端同样
    if (payload && typeof payload.code === 'number') {
      if (payload.code !== 0) {
        ElMessage.error(payload.message || '操作失败')
        if (payload.code === 40101) onUnauthorized()
        return Promise.reject(new ApiError(payload.code, payload.message))
      }
      return payload.data as never
    }
    return response.data
  },
  (error) => {
    const status: number | undefined = error?.response?.status
    const body = error?.response?.data as ApiResponse<unknown> | undefined
    const message = body?.message ?? friendlyMessage(status)
    ElMessage.error(message)
    if (status === 401) onUnauthorized()
    return Promise.reject(new ApiError(body?.code ?? status ?? -1, message))
  },
)

function friendlyMessage(status?: number): string {
  switch (status) {
    case 401: return '未登录或登录已过期'
    case 403: return '没有访问权限'
    case 413: return '文件大小超过限制'
    case 415: return '文件类型不支持'
    default: return '网络异常，请稍后重试'
  }
}

/** 业务/网络错误统一异常类型 */
export class ApiError extends Error {
  readonly code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

/** 类型化请求方法 */
export function get<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return http.get(url, { params, ...config }) as Promise<T>
}
export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, data, config) as Promise<T>
}
export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.put(url, data, config) as Promise<T>
}
export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return http.delete(url, config) as Promise<T>
}

export default http