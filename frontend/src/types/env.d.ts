/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 是否使用 Mock 数据（true 时不请求后端） */
  readonly VITE_USE_MOCK: string
  /** 接口基础路径 */
  readonly VITE_API_BASE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}