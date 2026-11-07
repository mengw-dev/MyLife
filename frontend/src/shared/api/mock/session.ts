/**
 * Mock 会话：所有模拟接口共享的登录态（与 Cookie 无关，纯内存）。
 */
import type { InternalAxiosRequestConfig } from 'axios'
import { mockUnauthorized } from './registry'

let currentUserId: string | null = null

export function mockLogin(userId: string): void {
  currentUserId = userId
}

export function mockLogout(): void {
  currentUserId = null
}

export function mockCurrentUserId(): string | null {
  return currentUserId
}

/** 要求已登录，否则抛出 401 错误 */
export function requireMockUser(config: InternalAxiosRequestConfig): string {
  if (!currentUserId) {
    throw mockUnauthorized(config)
  }
  return currentUserId
}