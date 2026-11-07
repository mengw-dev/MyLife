<script setup lang="ts">
/**
 * 知识分类管理：树形展示 + 新建/重命名/删除。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElButton, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { knowledgeApi } from '@/modules/knowledge/api/knowledge.api'
import { flatCategories } from '@/modules/knowledge/utils/category'
import CategoryTree from '@/modules/knowledge/components/CategoryTree.vue'
import AppDialog from '@/shared/components/AppDialog.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import type { KnowledgeCategory } from '@/shared/types/entities'

const loading = ref(true)
const loadFailed = ref(false)
const tree = ref<KnowledgeCategory[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({ name: '', parentId: '' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

const parentOptions = computed(() => {
  const exclude = dialogMode.value === 'edit' ? editingId.value : null
  return flatCategories(tree.value, exclude)
})

async function load() {
  loading.value = true
  loadFailed.value = false
  try {
    tree.value = await knowledgeApi.getCategoryTree()
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)

function openCreate(parentId: string | null = null) {
  dialogMode.value = 'create'
  editingId.value = null
  form.name = ''
  form.parentId = parentId ?? ''
  dialogVisible.value = true
}

function openEdit(category: KnowledgeCategory) {
  dialogMode.value = 'edit'
  editingId.value = category.id
  form.name = category.name
  form.parentId = category.parentId ?? ''
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (dialogMode.value === 'create') {
    await knowledgeApi.createCategory({ name: form.name, parentId: form.parentId || null })
    ElMessage.success('分类已创建')
  } else {
    await knowledgeApi.updateCategory(editingId.value!, { name: form.name, parentId: form.parentId || null })
    ElMessage.success('分类已更新')
  }
  dialogVisible.value = false
  await load()
}

async function onDelete(category: KnowledgeCategory) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${category.name}」吗？`, '删除分类', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await knowledgeApi.deleteCategory(category.id)
    ElMessage.success('分类已删除')
    await load()
  } catch {
    // 409 等业务错误已由拦截器提示
  }
}
</script>

<template>
  <section class="page">
    <header class="page-head">
      <h1 class="page-title">知识分类</h1>
      <ElButton type="primary" @click="openCreate()">新建根分类</ElButton>
    </header>

    <AppLoading v-if="loading" />
    <div v-else-if="loadFailed" class="state">
      <AppEmpty text="分类加载失败" />
      <ElButton class="state-btn" @click="load">重试</ElButton>
    </div>
    <div v-else-if="tree.length === 0" class="state">
      <AppEmpty text="还没有分类，先新建一个根分类吧" />
    </div>
    <div v-else class="tree-card">
      <CategoryTree
        :categories="tree"
        @new="openCreate($event.id)"
        @edit="openEdit"
        @delete="onDelete"
      />
    </div>

    <AppDialog
      :visible="dialogVisible"
      :title="dialogMode === 'create' ? '新建分类' : '重命名分类'"
      @close="dialogVisible = false"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="分类名称" prop="name">
          <ElInput v-model="form.name" placeholder="分类名称" />
        </ElFormItem>
        <ElFormItem label="父分类">
          <ElSelect v-model="form.parentId" placeholder="不选择则为顶级分类">
            <ElOption label="无（顶级分类）" :value="''" />
            <ElOption v-for="opt in parentOptions" :key="opt.id" :label="`${'　'.repeat(opt.depth)}${opt.name}`" :value="opt.id" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submit">保存</ElButton>
      </template>
    </AppDialog>
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
.tree-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 16px 18px;
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