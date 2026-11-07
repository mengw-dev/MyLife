/**
 * Mock 注册入口：集中登记全部模拟路由（各业务模块的 registerXxxMock 在此汇总）。
 */
import { registerAuthMock } from './modules/auth.mock'
import { registerTimelineMock } from './modules/timeline.mock'
// 各管理模块的 mock（diary/study/life-stage/event/knowledge/media/search/data）由对应模块实现并在此登记
import { registerDiaryMock } from './modules/diary.mock'
import { registerStudyMock } from './modules/study.mock'
import { registerLifeStageMock } from './modules/life-stage.mock'
import { registerEventMock } from './modules/event.mock'
import { registerKnowledgeMock } from './modules/knowledge.mock'
import { registerMediaMock } from './modules/media.mock'
import { registerSearchMock } from './modules/search.mock'
import { registerDataMock } from './modules/data.mock'

export function registerMock(): void {
  registerAuthMock()
  registerTimelineMock()
  registerDiaryMock()
  registerStudyMock()
  registerLifeStageMock()
  registerEventMock()
  registerKnowledgeMock()
  registerMediaMock()
  registerSearchMock()
  registerDataMock()
}