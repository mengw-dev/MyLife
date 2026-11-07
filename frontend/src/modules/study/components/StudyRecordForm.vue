<script setup lang="ts">
/**
 * 学习记录表单（弹窗内使用）：标题/内容/理解/问题/日期/关联笔记/阶段/标签。
 * 表单变更即时写入时间线草稿预览。
 */
import { onMounted, reactive, ref, watch } from 'vue'
import { ElButton, ElDatePicker, ElForm, ElFormItem, ElInput, ElOption, ElSelect } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { usePreviewStore } from '@/stores/preview'
import { lifeStageApi } from '@/modules/life-stage/api/life-stage.api'
import { fetchNoteOptions, type StudyFormValue, type StudyRecordPayload } from '../api/study.api'
import type { KnowledgeNote, LifeStage } from '@/shared/types/entities'

const props = defineProps<{
  mode: 'create' | 'edit'
  initial: StudyFormValue | null
  submitting?: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: StudyRecordPayload): void
  (e: 'cancel'): void
}>()

const preview = usePreviewStore()
const formRef = ref<FormInstance>()
const noteOptions = ref<KnowledgeNote[]>([])
const stageOptions = ref<LifeStage[]>([])

const form = reactive<StudyFormValue>({
  id: props.initial?.id,
  title: props.initial?.title ?? '',
  content: props.initial?.content ?? '',
  understanding: props.initial?.understanding ?? '',
  questions: props.initial?.questions ?? '',
  studyDate: props.initial?.studyDate ?? '',
  noteId: props.initial?.noteId ?? '',
  stageId: props.initial?.stageId ?? '',
  tagNames: props.initial?.tagNames ? [...props.initial.tagNames] : [],
})

const rules: FormRules = {
  content: [{ required: true, message: '请输入学习内容', trigger: 'blur' }],
  studyDate: [{ required: true, message: '请选择学习日期', trigger: 'change' }],
}

function updateDraft() {
  preview.setDraft({
    kind: 'study',
    mode: props.mode,
    id: props.mode === 'edit' ? form.id : undefined,
    date: form.studyDate || undefined,
    originDate: props.mode === 'edit' ? props.initial?.studyDate : undefined,
    title: form.title || undefined,
    stageId: form.stageId || null,
  })
}

watch([() => form.studyDate, () => form.title, () => form.stageId], updateDraft)

onMounted(async () => {
  updateDraft()
  const [notes, stages] = await Promise.all([
    fetchNoteOptions().catch(() => [] as KnowledgeNote[]),
    lifeStageApi.list().catch(() => [] as LifeStage[]),
  ])
  noteOptions.value = notes
  stageOptions.value = stages
})

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', {
    title: form.title || null,
    content: form.content,
    understanding: form.understanding || null,
    questions: form.questions || null,
    studyDate: form.studyDate,
    noteId: form.noteId || null,
    stageId: form.stageId || null,
    tagNames: form.tagNames,
  })
}
</script>

<template>
  <div class="study-form">
    <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <ElFormItem label="标题">
        <ElInput v-model="form.title" placeholder="可选，一句话概括" maxlength="80" />
      </ElFormItem>
      <ElFormItem label="学习内容" prop="content">
        <ElInput v-model="form.content" type="textarea" :rows="4" placeholder="今天学到了什么？" />
      </ElFormItem>
      <ElFormItem label="我的理解">
        <ElInput v-model="form.understanding" type="textarea" :rows="3" placeholder="把概念换成自己的话" />
      </ElFormItem>
      <ElFormItem label="存疑问题">
        <ElInput v-model="form.questions" type="textarea" :rows="2" placeholder="还没想明白的地方" />
      </ElFormItem>
      <ElFormItem label="学习日期" prop="studyDate">
        <ElDatePicker v-model="form.studyDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
      </ElFormItem>
      <ElFormItem label="关联笔记">
        <ElSelect v-model="form.noteId" clearable filterable placeholder="选择关联笔记" style="width: 100%">
          <ElOption v-for="n in noteOptions" :key="n.id" :label="n.title" :value="n.id" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="人生阶段">
        <ElSelect v-model="form.stageId" clearable placeholder="选择所属阶段" style="width: 100%">
          <ElOption v-for="s in stageOptions" :key="s.id" :label="s.name" :value="s.id" />
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
    </ElForm>

    <div class="form-actions">
      <ElButton :disabled="submitting" @click="emit('cancel')">取消</ElButton>
      <ElButton type="primary" :loading="submitting" @click="submit">{{ mode === 'edit' ? '保存' : '创建' }}</ElButton>
    </div>
  </div>
</template>

<style scoped>
.study-form {
  width: 100%;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
</style>