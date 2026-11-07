/**
 * 认证 API 与请求/响应类型（对齐 docs/02 §2）。
 */
import { get, post, put, del } from '@/shared/api/http'
import type { User } from '@/shared/types/entities'

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export const authApi = {
  csrf: () => get<{ token: string }>('/auth/csrf'),
  register: (payload: RegisterPayload) => post<User>('/auth/register', payload),
  login: (payload: LoginPayload) => post<User>('/auth/login', payload),
  logout: () => post<null>('/auth/logout'),
  me: () => get<User>('/auth/me'),
  changePassword: (payload: ChangePasswordPayload) => put<null>('/auth/password', payload),
  deleteAccount: () => del<null>('/account'),
}