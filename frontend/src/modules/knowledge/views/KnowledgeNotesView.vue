<script setup lang="ts">
/**
 * 知识笔记列表：分页 + 关键字/分类/标签筛选 + 增删改入口。
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import { knowledgeApi } from '@/modules/knowledge/api/knowledge.api'
import { flatCategories, type FlatCategory } from '@/modules/knowledge/utils/category'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppPagination from '@/shared/components/AppPagination.vue'
import type { KnowledgeNote } from '@/shared/types/entities'
import { PATHS } from '@/shared/constants/paths'
import { toDateKey } from '@/shared/utils/date'

const router = useRouter()

const loading = ref(true)
const failed = ref(false)
const notes = ref<KnowledgeNote[]>([])
const total = ref(0)
const categoryOptions = ref<FlatCategory[]>([])

const query = reactive({ page: 1, pageSize: 10, keyword: '', categoryId: '', tag: '' })

async function load() {
  loading.value = true
  failed.value = false
  try {
    const res = await knowledgeApi.listNotes({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      tag: query.tag || undefined,
    })
    notes.value = res.records
    total.value = res.total
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categoryOptions.value = flatCategories(await knowledgeApi.getCategoryTree())
  } catch {
    // 分类下拉加载失败不阻断列表
  }
}

onMounted(() => {
  load()
  loadCategories()
})

function search() {
  query.page = 1
  load()
}

function onPageChange(payload: { page: number; pageSize: number }) {
  query.page = payload.page
  query.pageSize = payload.pageSize
  load()
}

function addNote() {
  router.push(PATHS.noteNew)
}

function editNote(id: string) {
  router.push(PATHS.noteEdit(id))
}

async function removeNote(note: KnowledgeNote) {
  try {
    await ElMessageBox.confirm(`确定删除笔记「${note.title}」吗？删除后关联学习记录将保留但解除关联。`, '删除笔记', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await knowledgeApi.deleteNote(note.id)
    ElMessage.success('笔记已删除')
    load()
  } catch {
    // 业务错误已由拦截器提示
  }
}

function catName(id: string): string {
  return categoryOptions.value.find((c) => c.id === id)?.name ?? '未分类'
}

function fmtDate(iso: string): string {
  return toDateKey(new Date(iso))
}
</script>

<template>
  <section class="page">
    <header class="page-head">
      <h1 class="page-title">知识笔记</h1>
      <ElButton type="primary" @click="addNote">新建笔记</ElButton>
    </header>

    <div class="filter-bar">
      <ElInput v-model="query.keyword" placeholder="搜索标题/内容" clearable class="filter-keyword" @keyup.enter="search" @clear="search" />
      <ElSelect v-model="query.categoryId" placeholder="全部分类" clearable class="filter-select" @change="search">
        <ElOption v-for="opt in categoryOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
      </ElSelect>
      <ElInput v-model="query.tag" placeholder="按标签筛选" clearable class="filter-tag" @keyup.enter="search" @clear="search" />
      <ElButton @click="search">搜索</ElButton>
    </div>

    <AppLoading v-if="loading" />
    <div v-else-if="failed" class="state">
      <AppEmpty text="笔记加载失败" />
      <ElButton class="state-btn" @click="load">重试</ElButton>
    </div>
    <div v-else-if="notes.length === 0" class="state">
      <AppEmpty text="没有找到笔记，换个筛选条件或新建一篇吧" />
    </div>
    <ul v-else class="note-list">
      <li v-for="note in notes" :key="note.id" class="note-item">
        <div class="note-main">
          <h3 class="note-title">{{ note.title }}</h3>
          <p class="note-meta">
            <span class="meta-cat">{{ catName(note.categoryId) }}</span>
            <span v-if="note.tagNames.length" class="meta-tags">
              <span v-for="tag in note.tagNames" :key="tag" class="tag-chip">{{ tag }}</span>
            </span>
            <span class="meta-time">{{ fmtDate(note.updatedAt) }}</span>
          </p>
        </div>
        <div class="note-actions">
          <ElButton link type="primary" @click="editNote(note.id)">编辑</ElButton>
          <ElButton link type="danger" @click="removeNote(note)">删除</ElButton>
        </div>
      </li>
    </ul>

    <AppPagination
      v-if="!loading && total > 0"
      :page="query.page"
      :page-size="query.pageSize"
      :total="total"
      @change="onPageChange"
    />
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
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.filter-keyword {
  flex: 1 1 220px;
  min-width: 180px;
}
.filter-select {
  width: 160px;
}
.filter-tag {
  width: 160px;
}
.note-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
}
.note-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 14px 18px;
}
.note-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin: 0 0 6px;
}
.note-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--muted);
  margin: 0;
}
.meta-cat {
  color: var(--accent-deep);
}
.meta-tags {
  display: inline-flex;
  gap: 6px;
}
.tag-chip {
  background: var(--accent-soft);
  color: var(--accent-deep);
  border-radius: 999px;
  padding: 1px 9px;
  font-size: 11.5px;
}
.meta-time {
  color: var(--muted);
}
.note-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
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