<script setup lang="ts">
/**
 * 笔记编辑器：新建/编辑共用（/manage/knowledge/notes/new 与 /:id/edit）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElForm, ElFormItem, ElInput, ElMessage, ElOption, ElSelect } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { knowledgeApi } from '@/modules/knowledge/api/knowledge.api'
import { flatCategories, type FlatCategory } from '@/modules/knowledge/utils/category'
import MarkdownEditor from '@/modules/knowledge/components/MarkdownEditor.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import { useUnsavedGuard } from '@/shared/composables/useUnsavedGuard'
import { PATHS } from '@/shared/constants/paths'

const route = useRoute()
const router = useRouter()

const noteId = typeof route.params.id === 'string' ? route.params.id : ''
const isEdit = computed(() => noteId !== '')

const loading = ref(true)
const loadFailed = ref(false)
const categoryOptions = ref<FlatCategory[]>([])
const formRef = ref<FormInstance>()
const form = reactive({ title: '', categoryId: '', contentMarkdown: '', tagNames: [] as string[] })
const savedSnapshot = ref('')

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

function snapshot(): string {
  return JSON.stringify({
    title: form.title,
    categoryId: form.categoryId,
    contentMarkdown: form.contentMarkdown,
    tagNames: form.tagNames,
  })
}

const dirty = computed(() => snapshot() !== savedSnapshot.value)
useUnsavedGuard(() => dirty.value)

async function load() {
  loading.value = true
  loadFailed.value = false
  try {
    const [categories, note] = await Promise.all([
      knowledgeApi.getCategoryTree().then((t) => flatCategories(t)),
      isEdit.value ? knowledgeApi.getNote(noteId) : Promise.resolve(null),
    ])
    categoryOptions.value = categories
    if (note) {
      form.title = note.title
      form.categoryId = note.categoryId
      form.contentMarkdown = note.contentMarkdown
      form.tagNames = [...note.tagNames]
    }
    savedSnapshot.value = snapshot()
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload = {
    title: form.title,
    contentMarkdown: form.contentMarkdown,
    categoryId: form.categoryId,
    tagNames: form.tagNames,
  }
  if (isEdit.value) {
    await knowledgeApi.updateNote(noteId, payload)
    ElMessage.success('笔记已更新')
  } else {
    await knowledgeApi.createNote(payload)
    ElMessage.success('笔记已创建')
  }
  savedSnapshot.value = snapshot()
  router.push(PATHS.knowledgeNotes)
}

function cancel() {
  router.push(PATHS.knowledgeNotes)
}
</script>

<template>
  <section class="page">
    <header class="page-head">
      <h1 class="page-title">{{ isEdit ? '编辑笔记' : '新建笔记' }}</h1>
      <div class="head-actions">
        <ElButton @click="cancel">取消</ElButton>
        <ElButton type="primary" @click="save">保存</ElButton>
      </div>
    </header>

    <AppLoading v-if="loading" />
    <div v-else-if="loadFailed" class="state">
      <AppEmpty text="笔记加载失败" />
      <ElButton class="state-btn" @click="load">重试</ElButton>
    </div>
    <ElForm v-else ref="formRef" :model="form" :rules="rules" label-position="top" class="editor-form">
      <ElFormItem label="标题" prop="title">
        <ElInput v-model="form.title" placeholder="笔记标题" />
      </ElFormItem>
      <div class="form-row">
        <ElFormItem label="所属分类" prop="categoryId" class="form-cat">
          <ElSelect v-model="form.categoryId" placeholder="选择分类">
            <ElOption v-for="opt in categoryOptions" :key="opt.id" :label="`${'　'.repeat(opt.depth)}${opt.name}`" :value="opt.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="标签" class="form-tags">
          <ElSelect v-model="form.tagNames" multiple filterable allow-create default-first-option :reserve-keyword="false" placeholder="输入标签后回车">
            <ElOption v-for="tag in form.tagNames" :key="tag" :label="tag" :value="tag" />
          </ElSelect>
        </ElFormItem>
      </div>
      <ElFormItem label="内容">
        <MarkdownEditor v-model="form.contentMarkdown" />
      </ElFormItem>
    </ElForm>
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 500;
  color: var(--ink);
}
.head-actions {
  display: flex;
  gap: 8px;
}
.editor-form {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 20px 22px;
}
.form-row {
  display: flex;
  gap: 14px;
}
.form-cat {
  flex: 1;
}
.form-tags {
  flex: 1;
}
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.state-btn {
  align-self: center;
}
</style>