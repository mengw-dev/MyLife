/**
 * Mock Axios 适配器：VITE_USE_MOCK=true 时 http 客户端使用它，不发起真实网络请求。
 */
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { AxiosHeaders } from 'axios'
import { matchMock } from './registry'
import { mockFail } from './registry'

export const mockAdapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) => {
  const matched = matchMock(config)
  if (!matched) {
    throw mockFail(config, 404, 40401, '接口不存在')
  }
  let body: unknown = config.data
  if (typeof body === 'string' && body) {
    try { body = JSON.parse(body) } catch { body = null }
  }
  const query = extractQuery(config)
  const result = await matched.handler(matched.params, body, query, config)

  const response: AxiosResponse = {
    data: result.body,
    status: result.status,
    statusText: String(result.status),
    headers: new AxiosHeaders(),
    config,
  }
  return response
}

function extractQuery(config: InternalAxiosRequestConfig): Record<string, string> {
  const out: Record<string, string> = {}
  if (!config.url?.includes('?')) return out
  for (const [k, v] of new URLSearchParams(config.url.split('?')[1])) out[k] = v
  return out
}