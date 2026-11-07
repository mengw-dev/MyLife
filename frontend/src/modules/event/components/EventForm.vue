<script setup lang="ts">
/**
 * 重要事件表单（弹窗内使用）：标题/日期/描述/阶段/关联日记/标签/照片多选。
 * 表单变更即时写入时间线草稿预览。
 */
import { onMounted, reactive, ref, watch } from 'vue'
import { ElButton, ElDatePicker, ElForm, ElFormItem, ElInput, ElOption, ElSelect } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import AppImage from '@/shared/components/AppImage.vue'
import { mediaUrl } from '@/shared/utils/media'
import { usePreviewStore } from '@/stores/preview'
import { lifeStageApi } from '@/modules/life-stage/api/life-stage.api'
import { diaryApi, fetchMediaFirstPage } from '@/modules/diary/api/diary.api'
import type { EventFormValue, EventPayload } from '../api/event.api'
import type { Diary, LifeStage, MediaFile } from '@/shared/types/entities'

const props = defineProps<{
  mode: 'create' | 'edit'
  initial: EventFormValue | null
  submitting?: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: EventPayload): void
  (e: 'cancel'): void
}>()

const preview = usePreviewStore()
const formRef = ref<FormInstance>()
const stageOptions = ref<LifeStage[]>([])
const diaryOptions = ref<Diary[]>([])
const mediaOptions = ref<MediaFile[]>([])

const form = reactive<EventFormValue>({
  id: props.initial?.id,
  title: props.initial?.title ?? '',
  eventDate: props.initial?.eventDate ?? '',
  description: props.initial?.description ?? '',
  stageId: props.initial?.stageId ?? '',
  diaryId: props.initial?.diaryId ?? '',
  tagNames: props.initial?.tagNames ? [...props.initial.tagNames] : [],
  mediaIds: props.initial?.mediaIds ? [...props.initial.mediaIds] : [],
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入事件标题', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择事件日期', trigger: 'change' }],
}

function updateDraft() {
  preview.setDraft({
    kind: 'event',
    mode: props.mode,
    id: props.mode === 'edit' ? form.id : undefined,
    date: form.eventDate || undefined,
    originDate: props.mode === 'edit' ? props.initial?.eventDate : undefined,
    title: form.title || undefined,
    stageId: form.stageId || null,
  })
}

watch([() => form.eventDate, () => form.title, () => form.stageId], updateDraft)

onMounted(async () => {
  updateDraft()
  const [stages, diaries, media] = await Promise.all([
    lifeStageApi.list().catch(() => [] as LifeStage[]),
    diaryApi.list({ page: 1, pageSize: 100 }).then((r) => r.records).catch(() => [] as Diary[]),
    fetchMediaFirstPage().catch(() => [] as MediaFile[]),
  ])
  stageOptions.value = stages
  diaryOptions.value = diaries
  mediaOptions.value = media
})

function toggleMedia(id: string) {
  const i = form.mediaIds.indexOf(id)
  if (i >= 0) form.mediaIds.splice(i, 1)
  else form.mediaIds.push(id)
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', {
    title: form.title,
    eventDate: form.eventDate,
    description: form.description || null,
    stageId: form.stageId || null,
    diaryId: form.diaryId || null,
    mediaIds: form.mediaIds,
    tagNames: form.tagNames,
  })
}
</script>

<template>
  <div class="event-form">
    <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <ElFormItem label="事件标题" prop="title">
        <ElInput v-model="form.title" placeholder="一件值得记住的事" maxlength="80" />
      </ElFormItem>
      <ElFormItem label="事件日期" prop="eventDate">
        <ElDatePicker v-model="form.eventDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
      </ElFormItem>
      <ElFormItem label="描述">
        <ElInput v-model="form.description" type="textarea" :rows="3" placeholder="补充一些细节（可选）" />
      </ElFormItem>
      <ElFormItem label="人生阶段">
        <ElSelect v-model="form.stageId" clearable placeholder="选择所属阶段" style="width: 100%">
          <ElOption v-for="s in stageOptions" :key="s.id" :label="s.name" :value="s.id" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="关联日记">
        <ElSelect v-model="form.diaryId" clearable filterable placeholder="选择关联日记（可选）" style="width: 100%">
          <ElOption v-for="d in diaryOptions" :key="d.id" :label="d.title" :value="d.id" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="标签">
        <ElSelect
          v-model="form.tagNames"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          placeholder="输入标签后回车"
          style="width: 100%"
        >
          <ElOption v-for="t in form.tagNames" :key="t" :label="t" :value="t" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="照片">
        <div v-if="mediaOptions.length" class="media-picker">
          <button
            v-for="m in mediaOptions"
            :key="m.id"
            type="button"
            class="media-option"
            :class="{ selected: form.mediaIds.includes(m.id) }"
            @click="toggleMedia(m.id)"
          >
            <AppImage :src="mediaUrl(m.id)" :alt="m.originalName" ratio="1 / 1" />
            <span class="media-check" aria-hidden="true">{{ form.mediaIds.includes(m.id) ? '✓' : '' }}</span>
          </button>
        </div>
        <p v-else class="form-hint">暂无照片可选</p>
      </ElFormItem>
    </ElForm>

    <div class="form-actions">
      <ElButton :disabled="submitting" @click="emit('cancel')">取消</ElButton>
      <ElButton type="primary" :loading="submitting" @click="submit">{{ mode === 'edit' ? '保存' : '创建' }}</ElButton>
    </div>
  </div>
</template>

<style scoped>
.event-form {
  width: 100%;
}
.media-picker {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: 8px;
}
.media-option {
  position: relative;
  padding: 0;
  border: 2px solid transparent;
  border-radius: var(--radius);
  background: none;
  overflow: hidden;
  cursor: pointer;
}
.media-option.selected {
  border-color: var(--mark-event);
}
.media-check {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--mark-event);
  color: var(--surface);
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}
.form-hint {
  color: var(--muted);
  font-size: 12.5px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
</style>