<script setup lang="ts">
/**
 * 管理中心三栏布局：左侧导航 + 中间工作区 + 右侧时间线预览。
 * 宽屏默认展开预览（编辑器页面）；其余页面可用按钮展开；移动端预览收进抽屉。
 */
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElDrawer, ElMessageBox } from 'element-plus'
import TimelinePreview from '@/modules/timeline/components/TimelinePreview.vue'
import { usePreviewStore } from '@/stores/preview'
import { PATHS } from '@/shared/constants/paths'

const route = useRoute()
const router = useRouter()
const preview = usePreviewStore()

const previewOpen = ref(false)
const previewDocked = ref(false)
const isEditor = computed(() =>
  route.name === 'manage-diary-new' || route.name === 'manage-diary-edit' || route.name === 'manage-note-new' || route.name === 'manage-note-edit')
watch(isEditor, (v) => { previewDocked.value = v }, { immediate: true })

function goTimeline() {
  router.push(PATHS.timeline)
}

const menu = [
  { path: PATHS.diaries, label: '日记管理' },
  { path: PATHS.knowledge, label: '知识库' },
  { path: PATHS.studyRecords, label: '学习记录' },
  { path: PATHS.stages, label: '人生阶段' },
  { path: PATHS.events, label: '重要事件' },
  { path: PATHS.albums, label: '相册' },
  { path: PATHS.data, label: '数据管理' },
  { path: PATHS.settings, label: '设置' },
]

async function jumpToTimeline() {
  if (preview.hasDraft) {
    try {
      await ElMessageBox.confirm('存在未保存的修改，离开将丢失。仍要跳转吗？', '未保存的修改', { type: 'warning' })
    } catch {
      return
    }
  }
  router.push(PATHS.timeline)
}
</script>

<template>
  <div class="manage-layout">
    <aside class="manage-side">
      <button type="button" class="side-brand" @click="goTimeline">
        <span class="side-brand-mark">MyLife</span>
        <span class="side-brand-sub">管理中心</span>
      </button>
      <nav class="side-menu">
        <RouterLink
          v-for="item in menu"
          :key="item.path"
          :to="item.path"
          class="side-item"
          :class="{ active: route.path.startsWith(String(item.path)) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="side-foot">
        <button type="button" class="side-back" @click="jumpToTimeline">← 返回人生时间线</button>
        <button
          v-if="!isEditor"
          type="button"
          class="side-back mobile-only"
          @click="previewOpen = true"
        >时间线预览</button>
      </div>
    </aside>

    <main class="manage-main">
      <RouterView />
    </main>

    <!-- 宽屏预览：编辑页停靠展示，其余页面可手动展开 -->
    <aside v-if="isEditor || previewDocked" class="manage-preview">
      <div class="preview-head">
        <span class="preview-title">时间线预览</span>
        <span v-if="preview.hasDraft" class="preview-badge">含未保存改动</span>
      </div>
      <TimelinePreview compact />
      <button type="button" class="preview-full" @click="jumpToTimeline">查看完整时间线 →</button>
    </aside>

    <!-- 移动端抽屉预览 -->
    <ElDrawer v-model="previewOpen" direction="rtl" size="320px" title="时间线预览">
      <TimelinePreview compact />
      <button type="button" class="preview-full" @click="jumpToTimeline">查看完整时间线 →</button>
    </ElDrawer>
  </div>
</template>

<style scoped>
.manage-layout {
  display: grid;
  grid-template-columns: var(--sidebar-w) minmax(0, 1fr);
  min-height: 100vh;
  background: var(--bg);
}
.manage-layout:has(.manage-preview) {
  grid-template-columns: var(--sidebar-w) minmax(0, 1fr) var(--preview-w);
}
.manage-side {
  border-right: 1px solid var(--line);
  background: var(--bg-soft);
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.side-brand {
  background: none;
  border: none;
  text-align: left;
  padding: 0 8px;
}
.side-brand-mark {
  display: block;
  font-family: var(--font-serif);
  font-size: 18px;
  color: var(--ink);
}
.side-brand-sub {
  font-size: 12px;
  color: var(--muted);
}
.side-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.side-item {
  padding: 8px 12px;
  border-radius: var(--radius);
  color: var(--ink-2);
  font-size: 13.5px;
  transition: background var(--dur) ease, color var(--dur) ease;
}
.side-item:hover {
  background: var(--accent-soft);
  color: var(--accent-deep);
}
.side-item.active {
  background: var(--accent-soft);
  color: var(--accent-deep);
  font-weight: 600;
}
.side-foot {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.side-back {
  background: none;
  border: none;
  text-align: left;
  color: var(--muted);
  font-size: 12.5px;
  padding: 6px 8px;
}
.side-back:hover {
  color: var(--accent-deep);
}
.manage-main {
  min-width: 0;
  padding: 24px 28px 56px;
  overflow-x: hidden;
}
.manage-preview {
  border-left: 1px solid var(--line);
  background: var(--bg-soft);
  padding: 14px 14px 20px;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.preview-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.preview-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
}
.preview-badge {
  font-size: 11px;
  color: var(--warning);
  border: 1px dashed var(--warning);
  padding: 1px 8px;
  border-radius: 999px;
}
.preview-full {
  background: none;
  border: none;
  color: var(--accent-deep);
  font-size: 12.5px;
  text-align: center;
  padding: 8px;
}
.mobile-only {
  display: none;
}

@media (max-width: 1100px) {
  .manage-layout,
  .manage-layout:has(.manage-preview) {
    grid-template-columns: 64px minmax(0, 1fr);
  }
  .side-brand, .side-item {
    font-size: 0;
  }
  .side-brand-sub { display: none; }
  .side-back { font-size: 0; height: 22px; }
  .manage-preview { display: none; }
  .mobile-only { display: block; }
}
</style>