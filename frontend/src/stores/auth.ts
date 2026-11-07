/**
 * 用户状态：登录态 / 当前用户 / 登录注册退出（真实与 Mock 共用）。
 */
import { defineStore } from 'pinia'
import { authApi } from '@/modules/auth/api/auth.api'
import type { User } from '@/shared/types/entities'

interface AuthState {
  user: User | null
  initialized: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    initialized: false,
  }),

  getters: {
    isLoggedIn: (state) => state.user !== null,
    displayName: (state) => state.user?.nickname || state.user?.username || '',
  },

  actions: {
    /** 应用启动/刷新时静默恢复登录态（401 不弹全局提示由调用方处理） */
    async init() {
      if (this.initialized) return
      try {
        this.user = await authApi.me()
      } catch {
        this.user = null
      } finally {
        this.initialized = true
      }
    },

    async login(username: string, password: string) {
      this.user = await authApi.login({ username, password })
    },

    async register(username: string, password: string, nickname?: string) {
      this.user = await authApi.register({ username, password, nickname })
    },

    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.user = null
      }
    },
  },
})