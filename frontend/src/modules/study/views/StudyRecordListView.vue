<script setup lang="ts">
/**
 * 学习记录列表：分页 + 新建/编辑弹窗 + 删除确认。
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
import StudyRecordForm from '../components/StudyRecordForm.vue'
import { studyApi, type StudyFormValue, type StudyRecordPayload } from '../api/study.api'
import type { StudyRecord } from '@/shared/types/entities'

const preview = usePreviewStore()
const timeline = useTimelineStore()

const loading = ref(false)
const error = ref(false)
const list = ref<StudyRecord[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogOpen = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const dialogInitial = ref<StudyFormValue | null>(null)
const submitting = ref(false)

async function load() {
  loading.value = true
  error.value = false
  try {
    const res = await studyApi.list({ page: page.value, pageSize: pageSize.value })
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
    content: '',
    understanding: '',
    questions: '',
    studyDate: todayKey(),
    noteId: '',
    stageId: '',
    tagNames: [],
  }
  dialogOpen.value = true
}

function openEdit(r: StudyRecord) {
  dialogMode.value = 'edit'
  dialogInitial.value = {
    id: r.id,
    title: r.title ?? '',
    content: r.content,
    understanding: r.understanding ?? '',
    questions: r.questions ?? '',
    studyDate: r.studyDate,
    noteId: r.noteId ?? '',
    stageId: r.stageId ?? '',
    tagNames: [...r.tagNames],
  }
  dialogOpen.value = true
}

function closeDialog() {
  preview.discard()
  dialogOpen.value = false
  dialogInitial.value = null
}

async function onSubmit(payload: StudyRecordPayload) {
  submitting.value = true
  preview.markSaving()
  try {
    if (dialogMode.value === 'edit') {
      await studyApi.update(dialogInitial.value!.id!, payload)
    } else {
      await studyApi.create(payload)
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

async function removeRecord(r: StudyRecord) {
  try {
    await ElMessageBox.confirm('确定删除这条学习记录吗？', '删除学习记录', { type: 'warning' })
  } catch {
    return
  }
  try {
    await studyApi.remove(r.id)
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
        <h1 class="page-title">学习记录</h1>
        <p class="page-sub">共 {{ total }} 条记录</p>
      </div>
      <ElButton type="primary" @click="openCreate">新建学习记录</ElButton>
    </header>

    <AppLoading v-if="loading" text="正在加载学习记录…" />
    <div v-else-if="error" class="state-error" role="status">
      <p>加载失败，请稍后重试</p>
      <ElButton @click="load">重试</ElButton>
    </div>
    <AppEmpty v-else-if="!list.length" text="还没有学习记录，记下今天学到的东西吧" />
    <div v-else class="record-list">
      <article v-for="r in list" :key="r.id" class="record-card">
        <div class="card-main">
          <h2 class="card-title">{{ r.title || '学习记录' }}</h2>
          <p class="card-meta">
            <span class="date">{{ r.studyDate }}</span>
            <span v-if="r.noteTitleSnapshot" class="note">{{ r.noteTitleSnapshot }}</span>
          </p>
          <p class="card-excerpt">{{ r.content }}</p>
          <p v-if="r.tagNames?.length" class="card-tags">
            <span v-for="t in r.tagNames" :key="t" class="tag"># {{ t }}</span>
          </p>
        </div>
        <div class="card-actions">
          <ElButton size="small" @click="openEdit(r)">编辑</ElButton>
          <ElButton size="small" type="danger" @click="removeRecord(r)">删除</ElButton>
        </div>
      </article>
    </div>

    <AppPagination :page="page" :page-size="pageSize" :total="total" @change="onPageChange" />

    <AppDialog :visible="dialogOpen" :title="dialogMode === 'edit' ? '编辑学习记录' : '新建学习记录'" width="620px" @close="closeDialog">
      <StudyRecordForm
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
.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}
.record-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-left: 3px solid var(--mark-study);
  border-radius: var(--radius);
  box-shadow: var(--shadow-panel);
  padding: 16px 18px;
}
.card-title {
  font-size: 15px;
}
.card-meta {
  margin-top: 6px;
  display: flex;
  gap: 12px;
  font-size: 12.5px;
  color: var(--muted);
}
.card-meta .date {
  font-family: var(--font-serif);
  color: var(--ink-2);
}
.card-excerpt {
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
  color: var(--accent-green);
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