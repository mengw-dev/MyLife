<script setup lang="ts">
/**
 * 人生阶段管理：阶段卡片列表 + 新建/编辑弹窗（起止日期/介绍/排序）+ 删除确认。
 * 开始日期晚于结束日期时进行前端校验报错。
 */
import { onMounted, reactive, ref, watch } from 'vue'
import { ElButton, ElDatePicker, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import AppDialog from '@/shared/components/AppDialog.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import { usePreviewStore } from '@/stores/preview'
import { useTimelineStore } from '@/modules/timeline/stores/timeline'
import { lifeStageApi, type LifeStagePayload } from '../api/life-stage.api'
import type { LifeStage } from '@/shared/types/entities'

const preview = usePreviewStore()
const timeline = useTimelineStore()

const loading = ref(false)
const error = ref(false)
const list = ref<LifeStage[]>([])

const dialogOpen = ref(false)
const editId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  startDate: '',
  endDate: '',
  description: '',
  sortOrder: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入阶段名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  error.value = false
  try {
    list.value = await lifeStageApi.list()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function updateDraft() {
  preview.setDraft({
    kind: 'stage',
    mode: editId.value ? 'edit' : 'create',
    id: editId.value ?? undefined,
    startDate: form.startDate,
    endDate: form.endDate || null,
    title: form.name || undefined,
  })
}

watch([() => form.startDate, () => form.endDate, () => form.name], updateDraft)

function openCreate() {
  editId.value = null
  form.name = ''
  form.startDate = ''
  form.endDate = ''
  form.description = ''
  form.sortOrder = list.value.length + 1
  dialogOpen.value = true
  updateDraft()
}

function openEdit(s: LifeStage) {
  editId.value = s.id
  form.name = s.name
  form.startDate = s.startDate
  form.endDate = s.endDate ?? ''
  form.description = s.description ?? ''
  form.sortOrder = s.sortOrder
  dialogOpen.value = true
  updateDraft()
}

function closeDialog() {
  preview.discard()
  dialogOpen.value = false
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.startDate && form.endDate && form.startDate > form.endDate) {
    ElMessage.error('开始日期不能晚于结束日期')
    return
  }
  const payload: LifeStagePayload = {
    name: form.name,
    startDate: form.startDate,
    endDate: form.endDate || null,
    description: form.description || null,
    sortOrder: form.sortOrder,
  }
  submitting.value = true
  preview.markSaving()
  try {
    if (editId.value) await lifeStageApi.update(editId.value, payload)
    else await lifeStageApi.create(payload)
    preview.commit()
    await timeline.refreshCurrent()
    ElMessage.success(editId.value ? '已保存' : '已创建')
    dialogOpen.value = false
    load()
  } catch {
    if (preview.draft) preview.setDraft({ ...preview.draft })
  } finally {
    submitting.value = false
  }
}

async function removeStage(s: LifeStage) {
  try {
    await ElMessageBox.confirm(`确定删除阶段「${s.name}」吗？其下的日记、学习记录和事件将保留。`, '删除阶段', { type: 'warning' })
  } catch {
    return
  }
  try {
    await lifeStageApi.remove(s.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  }
}

function dateRange(s: LifeStage): string {
  return s.endDate ? `${s.startDate} ~ ${s.endDate}` : `${s.startDate} ~ 至今`
}

onMounted(load)
</script>

<template>
  <div class="list-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">人生阶段</h1>
        <p class="page-sub">共 {{ list.length }} 个阶段</p>
      </div>
      <ElButton type="primary" @click="openCreate">新建阶段</ElButton>
    </header>

    <AppLoading v-if="loading" text="正在加载阶段…" />
    <div v-else-if="error" class="state-error" role="status">
      <p>加载失败，请稍后重试</p>
      <ElButton @click="load">重试</ElButton>
    </div>
    <AppEmpty v-else-if="!list.length" text="还没有人生阶段，先划分一段光阴吧" />
    <div v-else class="stage-grid">
      <article v-for="s in list" :key="s.id" class="stage-card">
        <div class="card-head">
          <h2 class="card-title">{{ s.name }}</h2>
          <span class="card-order">#{{ s.sortOrder }}</span>
        </div>
        <p class="card-range">{{ dateRange(s) }}</p>
        <p v-if="s.description" class="card-desc">{{ s.description }}</p>
        <div class="card-actions">
          <ElButton size="small" @click="openEdit(s)">编辑</ElButton>
          <ElButton size="small" type="danger" @click="removeStage(s)">删除</ElButton>
        </div>
      </article>
    </div>

    <AppDialog :visible="dialogOpen" :title="editId ? '编辑阶段' : '新建阶段'" width="520px" @close="closeDialog">
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="阶段名称" prop="name">
          <ElInput v-model="form.name" placeholder="例如：大学、职场初年" maxlength="40" />
        </ElFormItem>
        <ElFormItem label="起止日期">
          <div class="date-range-row">
            <ElDatePicker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="flex: 1" />
            <span class="range-sep">至</span>
            <ElDatePicker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期（可留空）" style="flex: 1" clearable />
          </div>
        </ElFormItem>
        <ElFormItem label="介绍">
          <ElInput v-model="form.description" type="textarea" :rows="3" placeholder="这一阶段的注脚（可选）" />
        </ElFormItem>
        <ElFormItem label="排序">
          <ElInputNumber v-model="form.sortOrder" :min="1" :step="1" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton :disabled="submitting" @click="closeDialog">取消</ElButton>
        <ElButton type="primary" :loading="submitting" @click="submit">保存</ElButton>
      </template>
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
.stage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
  margin-top: 18px;
}
.stage-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}
.card-title {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 500;
}
.card-order {
  font-size: 11.5px;
  color: var(--muted);
}
.card-range {
  font-size: 12.5px;
  color: var(--accent-deep);
}
.card-desc {
  font-size: 13px;
  color: var(--ink-2);
  flex: 1;
}
.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.date-range-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.range-sep {
  color: var(--muted);
  font-size: 12.5px;
}
</style>