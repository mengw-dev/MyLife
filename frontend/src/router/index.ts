import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { setupGuards } from './guards'
import { setOnUnauthorized } from '@/shared/api/http'

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, saved) {
    // 时间线层级浏览尽量保留滚动位置
    if (saved) return saved
    return { top: 0 }
  },
})

setupGuards(router)

// 401 统一回登录（由 http 拦截器触发，避免循环依赖）
setOnUnauthorized(() => {
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
})

export default router