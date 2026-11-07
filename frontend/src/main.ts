import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from '@/app/App.vue'
import router from '@/router'
import { registerMock } from '@/shared/api/mock'

import '@/styles/variables.css'
import '@/styles/reset.css'
import '@/styles/global.css'
import '@/styles/transitions.css'

// Mock 模式：注册全部模拟接口（VITE_USE_MOCK=true 时 Http 客户端走 Mock 适配器）
if (import.meta.env.VITE_USE_MOCK === 'true') {
  registerMock()
}

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')