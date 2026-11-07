<script setup lang="ts">
/**
 * 日记编辑器：/manage/diaries/new 与 /manage/diaries/:id/edit 共用。
 * 负责加载初值、保存（markSaving → 成功 commit + 刷新时间线）、离开守卫。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLoading from '@/shared/components/AppLoading.vue'
import { useUnsavedGuard } from '@/shared/composables/useUnsavedGuard'
import { usePreviewStore } from '@/stores/preview'
import { useTimelineStore } from '@/modules/timeline/stores/timeline'
import { PATHS } from '@/shared/constants/paths'
import { todayKey } from '@/shared/utils/date'
import DiaryForm from '../components/DiaryForm.vue'
import { diaryApi, type DiaryFormValue, type DiaryPayload } from '../api/diary.api'
import type { Diary } from '@/shared/types/entities'

const route = useRoute()
const router = useRouter()
const preview = usePreviewStore()
const timeline = useTimelineStore()

const isEdit = computed(() => route.name === 'manage-diary-edit')
const id = computed(() => String(route.params.id ?? ''))
const loading = ref(false)
const submitting = ref(false)
const initial = ref<DiaryFormValue | null>(null)

useUnsavedGuard(() => preview.hasDraft)

function toFormValue(d: Diary): DiaryFormValue {
  return {
    id: d.id,
    title: d.title,
    content: d.content,
    recordDate: d.recordDate,
    stageId: d.stageId ?? '',
    tagNames: [...d.tagNames],
    mediaIds: [...d.mediaIds],
  }
}

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    try {
      const d = await diaryApi.get(id.value)
      initial.value = toFormValue(d)
    } finally {
      loading.value = false
    }
  } else {
    const rd = typeof route.query.recordDate === 'string' ? route.query.recordDate : todayKey()
    initial.value = { title: '', content: '', recordDate: rd, stageId: '', tagNames: [], mediaIds: [] }
  }
})

async function onSubmit(payload: DiaryPayload) {
  submitting.value = true
  preview.markSaving()
  try {
    if (isEdit.value) await diaryApi.update(id.value, payload)
    else await diaryApi.create(payload)
    preview.commit()
    await timeline.refreshCurrent()
    ElMessage.success(isEdit.value ? '已保存' : '已创建')
    router.push(PATHS.diaries)
  } catch {
    // 失败保留草稿，saving 状态复位（错误提示由 http 拦截器统一处理）
    if (preview.draft) preview.setDraft({ ...preview.draft })
  } finally {
    submitting.value = false
  }
}

function onCancel() {
  preview.discard()
  router.push(PATHS.diaries)
}
</script>

<template>
  <div class="editor-page">
    <nav class="crumbs" aria-label="面包屑">
      <RouterLink :to="PATHS.diaries">日记管理</RouterLink>
      <span class="sep">/</span>
      <span class="current">{{ isEdit ? '编辑日记' : '新建日记' }}</span>
    </nav>
    <h1 class="page-title">{{ isEdit ? '编辑日记' : '新建日记' }}</h1>

    <AppLoading v-if="loading" text="正在读取日记…" />
    <DiaryForm
      v-else-if="initial"
      :mode="isEdit ? 'edit' : 'create'"
      :initial="initial"
      :submitting="submitting"
      @submit="onSubmit"
      @cancel="onCancel"
    />
  </div>
</template>

<style scoped>
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
  margin-bottom: 14px;
}
.sep { color: var(--line-strong); }
.current { color: var(--ink-2); font-weight: 500; }
.page-title {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 500;
  margin-bottom: 20px;
}
</style>