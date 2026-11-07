<script setup lang="ts">
/**
 * 重要事件列表：分页 + 新建/编辑弹窗 + 删除确认（视觉略醒目）。
 */
import { onMounted, ref } from 'vue'
import { ElButton, ElMessage, ElMessageBox } from 'element-plus'
import AppDialog from '@/shared/components/AppDialog.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppPagination from '@/shared/components/AppPagination.vue'
import { usePreviewStore } from '@/stores/preview'
import { useTimelineStore } from '@/modules/timeline/stores/timeline'
import { todayKey } from '@/shared/utils/date'
import EventForm from '../components/EventForm.vue'
import { eventApi, type EventFormValue, type EventPayload } from '../api/event.api'
import type { ImportantEvent } from '@/shared/types/entities'

const preview = usePreviewStore()
const timeline = useTimelineStore()

const loading = ref(false)
const error = ref(false)
const list = ref<ImportantEvent[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogOpen = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const dialogInitial = ref<EventFormValue | null>(null)
const submitting = ref(false)

async function load() {
  loading.value = true
  error.value = false
  try {
    const res = await eventApi.list({ page: page.value, pageSize: pageSize.value })
    list.value = res.records
    total.value = res.total
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function onPageChange(payload: { page: number; pageSize: number }) {
  page.value = payload.page
  pageSize.value = payload.pageSize
  load()
}

function openCreate() {
  dialogMode.value = 'create'
  dialogInitial.value = {
    title: '',
    eventDate: todayKey(),
    description: '',
    stageId: '',
    diaryId: '',
    tagNames: [],
    mediaIds: [],
  }
  dialogOpen.value = true
}

function openEdit(e: ImportantEvent) {
  dialogMode.value = 'edit'
  dialogInitial.value = {
    id: e.id,
    title: e.title,
    eventDate: e.eventDate,
    description: e.description ?? '',
    stageId: e.stageId ?? '',
    diaryId: e.diaryId ?? '',
    tagNames: [...e.tagNames],
    mediaIds: [...e.mediaIds],
  }
  dialogOpen.value = true
}

function closeDialog() {
  preview.discard()
  dialogOpen.value = false
  dialogInitial.value = null
}

async function onSubmit(payload: EventPayload) {
  submitting.value = true
  preview.markSaving()
  try {
    if (dialogMode.value === 'edit') {
      await eventApi.update(dialogInitial.value!.id!, payload)
    } else {
      await eventApi.create(payload)
    }
    preview.commit()
    await timeline.refreshCurrent()
    ElMessage.success(dialogMode.value === 'edit' ? '已保存' : '已创建')
    dialogOpen.value = false
    dialogInitial.value = null
    load()
  } catch {
    if (preview.draft) preview.setDraft({ ...preview.draft })
  } finally {
    submitting.value = false
  }
}

async function removeEvent(e: ImportantEvent) {
  try {
    await ElMessageBox.confirm(`确定删除事件「${e.title}」吗？`, '删除事件', { type: 'warning' })
  } catch {
    return
  }
  try {
    await eventApi.remove(e.id)
    ElMessage.success('已删除')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  }
}

onMounted(load)
</script>

<template>
  <div class="list-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">重要事件</h1>
        <p class="page-sub">共 {{ total }} 个事件</p>
      </div>
      <ElButton type="primary" @click="openCreate">新建事件</ElButton>
    </header>

    <AppLoading v-if="loading" text="正在加载事件…" />
    <div v-else-if="error" class="state-error" role="status">
      <p>加载失败，请稍后重试</p>
      <ElButton @click="load">重试</ElButton>
    </div>
    <AppEmpty v-else-if="!list.length" text="还没有重要事件，标记一个值得纪念的时刻" />
    <div v-else class="event-list">
      <article v-for="e in list" :key="e.id" class="event-card">
        <div class="card-main">
          <h2 class="card-title">{{ e.title }}</h2>
          <p class="card-date">{{ e.eventDate }}</p>
          <p v-if="e.description" class="card-desc">{{ e.description }}</p>
          <p v-if="e.tagNames?.length" class="card-tags">
            <span v-for="t in e.tagNames" :key="t" class="tag"># {{ t }}</span>
          </p>
        </div>
        <div class="card-actions">
          <ElButton size="small" @click="openEdit(e)">编辑</ElButton>
          <ElButton size="small" type="danger" @click="removeEvent(e)">删除</ElButton>
        </div>
      </article>
    </div>

    <AppPagination :page="page" :page-size="pageSize" :total="total" @change="onPageChange" />

    <AppDialog :visible="dialogOpen" :title="dialogMode === 'edit' ? '编辑重要事件' : '新建重要事件'" width="620px" @close="closeDialog">
      <EventForm
        v-if="dialogInitial"
        :mode="dialogMode"
        :initial="dialogInitial"
        :submitting="submitting"
        @submit="onSubmit"
        @cancel="closeDialog"
      />
    </AppDialog>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.page-title {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 500;
}
.page-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12.5px;
}
.state-error {
  padding: 48px 16px;
  text-align: center;
  color: var(--muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}
.event-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-left: 4px solid var(--mark-event);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  padding: 16px 18px;
}
.card-title {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 500;
}
.card-date {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--mark-event);
  font-weight: 600;
}
.card-desc {
  margin-top: 8px;
  color: var(--ink-2);
  font-size: 13.5px;
  white-space: pre-wrap;
}
.card-tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.tag {
  font-size: 11.5px;
  color: var(--accent-deep);
  background: var(--bg-soft);
  border-radius: 999px;
  padding: 1px 10px;
}
.card-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
}
</style>