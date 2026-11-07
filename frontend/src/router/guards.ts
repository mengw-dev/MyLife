/**
 * 全局路由守卫：登录校验集中在此处，页面内不得重复检查（DEV-NOTES §2）。
 */
import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { PATHS } from '@/shared/constants/paths'

/** 无需登录的路径 */
const PUBLIC_PATHS: string[] = [PATHS.login, PATHS.register]

export function setupGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()
    await auth.init()

    if (!auth.isLoggedIn && !PUBLIC_PATHS.includes(to.path)) {
      return { path: PATHS.login, query: to.fullPath !== PATHS.timeline ? { redirect: to.fullPath } : {} }
    }
    if (auth.isLoggedIn && PUBLIC_PATHS.includes(to.path)) {
      return { path: PATHS.timeline }
    }

    // 管理中心 → 时间线的跳转携带“回到管理”信息时，保留原路由回跳能力
    if (to.query.redirect && auth.isLoggedIn && PUBLIC_PATHS.includes(to.path)) {
      const target = String(to.query.redirect)
      if (target.startsWith('/')) return { path: target }
    }
    return true
  })

  router.afterEach((to) => {
    if (import.meta.env.DEV && to.name) {
      document.title = `${String(to.meta.title ?? 'MyLife')} · MyLife`
    }
  })

  // 供 http 拦截器在 401 时跳转登录
  void ElMessage
}