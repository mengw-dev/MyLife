/**
 * 草稿预览状态机（DEV-NOTES §5）：管理中心编辑页与时间线预览的唯一桥梁。
 */
import { defineStore } from 'pinia'

/** 时间线可预览的草稿（按记录类型区分） */
export interface TimelineDraft {
  /** 草稿类型：决定预览中如何绘制 */
  kind: 'diary' | 'study' | 'event' | 'stage'
  /** 编辑已有记录时为该记录 id（用于在旧位置保留“原始记录”示意） */
  id?: string
  mode: 'create' | 'edit'
  /** 草稿将要归属的日期（diary→recordDate / study→studyDate / event→eventDate） */
  date?: string
  /** 编辑场景：记录原始日期（原位置仍显示已保存记录） */
  originDate?: string
  /** 人生阶段草稿的时间范围 */
  startDate?: string
  endDate?: string | null
  /** 展示用标题 */
  title?: string
  /** 用户临时选取的人生阶段 id */
  stageId?: string | null
}

interface PreviewState {
  draft: TimelineDraft | null
  saving: boolean
}

export const usePreviewStore = defineStore('preview', {
  state: (): PreviewState => ({
    draft: null,
    saving: false,
  }),

  getters: {
    hasDraft: (state) => state.draft !== null,
  },

  actions: {
    /** 表单变更 → 立即更新本地预览（不请求后端） */
    setDraft(draft: TimelineDraft | null) {
      this.draft = draft
      if (draft && this.saving) this.saving = false
    },

    /** 标记正在提交（保存中状态） */
    markSaving() {
      this.saving = true
    },

    /** 保存成功 → 清除草稿（正式数据刷新由调用方触发 timeline store） */
    commit() {
      this.draft = null
      this.saving = false
    },

    /** 取消/保存失败 → 保留或撤销草稿 */
    discard() {
      this.draft = null
      this.saving = false
    },
  },
})