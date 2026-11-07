/**
 * 认证接口 Mock（对齐 docs/02 §2）：注册/登录/登出/me/改密/注销 + CSRF。
 */
import type { InternalAxiosRequestConfig } from 'axios'
import { registerMock, ok, mockFail } from '@/shared/api/mock/registry'
import { mockDb, nextId } from '@/shared/api/mock/db'
import { mockLogin, mockLogout, mockCurrentUserId, requireMockUser } from '@/shared/api/mock/session'
import type { User } from '@/shared/types/entities'

function toUserVO(user: User) {
  return { id: user.id, username: user.username, nickname: user.nickname, createdAt: user.createdAt }
}

export function registerAuthMock(): void {
  registerMock('GET', '/auth/csrf', () => ok({ token: 'mock-csrf-token' }))

  registerMock('POST', '/auth/register', (_, body) => {
    const { username, password, nickname } = (body ?? {}) as { username?: string; password?: string; nickname?: string }
    if (!username || !password) {
      return { status: 400, body: { code: 40001, message: '参数校验失败', data: null } }
    }
    if (mockDb.users.some((u) => u.username === username)) {
      return { status: 409, body: { code: 40901, message: '用户名已被注册', data: null } }
    }
    const user: User = { id: nextId(), username, nickname: nickname || username, createdAt: new Date().toISOString() }
    mockDb.users.push(user)
    mockDb.userPasswords[username] = password
    return { status: 201, body: { code: 0, message: '注册成功', data: toUserVO(user) } }
  })

  registerMock('POST', '/auth/login', (_, body, _q, cfg) => {
    const { username, password } = (body ?? {}) as { username?: string; password?: string }
    if (!username || mockDb.userPasswords[username] !== password) {
      throw mockFail(cfg as unknown as InternalAxiosRequestConfig, 401, 40101, '用户名或密码错误')
    }
    mockLogin(mockDb.users.find((u) => u.username === username)!.id)
    const user = mockDb.users.find((u) => u.username === username)!
    return ok(toUserVO(user), 200)
  })

  registerMock('POST', '/auth/logout', () => {
    mockLogout()
    return ok(null)
  })

  registerMock('GET', '/auth/me', (_p, _b, _q, cfg) => {
    const id = requireMockUser(cfg as unknown as InternalAxiosRequestConfig)
    const user = mockDb.users.find((u) => u.id === id)
    return ok(user ? toUserVO(user) : null)
  })

  registerMock('PUT', '/auth/password', (_, body) => {
    const { oldPassword, newPassword } = (body ?? {}) as { oldPassword?: string; newPassword?: string }
    const id = mockCurrentUserId()
    const user = mockDb.users.find((u) => u.id === id)
    if (!user || mockDb.userPasswords[user.username] !== oldPassword) {
      return { status: 400, body: { code: 40001, message: '原密码错误', data: null } }
    }
    mockDb.userPasswords[user.username] = newPassword!
    mockLogout()
    return ok(null)
  })

  registerMock('DELETE', '/account', () => {
    const id = mockCurrentUserId()
    mockDb.users = mockDb.users.filter((u) => u.id !== id)
    mockDb.diaries = mockDb.diaries.filter((d) => d.stageId !== 'stale')
    mockLogout()
    return ok(null)
  })
}