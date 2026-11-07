/** 媒体展示地址：Mock 模式用占位图，真实模式走鉴权内容接口。 */
const MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export function mediaUrl(id: string): string {
  if (MOCK) {
    return `https://picsum.photos/seed/mylife-media-${id}/640/420`
  }
  return `${import.meta.env.VITE_API_BASE}/media/${id}/content`
}