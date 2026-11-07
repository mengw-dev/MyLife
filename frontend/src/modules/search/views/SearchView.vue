<script setup lang="ts">
/**
 * 搜索页：跨日记/笔记/学习/事件的关键词检索，支持类型筛选与命中片段高亮。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { search } from '../api/search.api'
import type { SearchItem, SearchType } from '../api/search.api'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppPagination from '@/shared/components/AppPagination.vue'
import { PATHS } from '@/shared/constants/paths'

const router = useRouter()

const q = ref('')
const type = ref<'' | SearchType>('')
const page = ref(1)
const pageSize = ref(10)
const records = ref<SearchItem[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref(false)
const searched = ref(false)

const typeOptions: Array<{ label: string; value: '' | SearchType }> = [
  { label: '全部', value: '' },
  { label: '日记', value: 'diary' },
  { label: '笔记', value: 'note' },
  { label: '学习', value: 'study' },
  { label: '事件', value: 'event' },
]

const typeLabel: Record<SearchType, string> = {
  diary: '日记',
  note: '笔记',
  study: '学习',
  event: '事件',
}

async function runSearch(p: number) {
  const keyword = q.value.trim()
  if (!keyword) {
    searched.value = false
    records.value = []
    total.value = 0
    return
  }
  loading.value = true
  error.value = false
  try {
    const res = await search({ q: keyword, types: type.value, page: p, pageSize: pageSize.value })
    records.value = res.records
    total.value = res.total
    searched.value = true
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function submitSearch() {
  page.value = 1
  runSearch(1)
}

function onPageChange(payload: { page: number; pageSize: number }) {
  page.value = payload.page
  pageSize.value = payload.pageSize
  runSearch(payload.page)
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function highlight(text: string, keyword: string): string {
  const escaped = escapeHtml(text)
  if (!keyword) return escaped
  const safe = escapeHtml(keyword).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escaped.replace(new RegExp(safe, 'gi'), (m) => `<mark>${m}</mark>`)
}

function open(item: SearchItem) {
  const targets: Record<SearchType, string> = {
    diary: PATHS.diaryEdit(item.id),
    note: PATHS.noteEdit(item.id),
    study: PATHS.studyRecords,
    event: PATHS.events,
  }
  router.push(targets[item.type])
}
</script>

<template>
  <div class="app-page search-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">搜索</h1>
        <p class="page-sub">在日记、笔记、学习记录与重要事件中查找</p>
      </div>
    </header>

    <div class="search-bar">
      <ElInput
        v-model="q"
        size="large"
        clearable
        placeholder="输入关键词，例如：线程池、毕业、索引…"
        @keyup.enter="submitSearch"
      />
      <ElButton type="primary" size="large" @click="submitSearch">搜索</ElButton>
    </div>

    <ElRadioGroup v-model="type" class="type-filter" @change="submitSearch">
      <ElRadioButton v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</ElRadioButton>
    </ElRadioGroup>

    <AppEmpty v-if="!searched && !loading" text="输入关键词，开始搜索你的人生记录" />
    <AppLoading v-else-if="loading" text="正在检索…" />
    <div v-else-if="error" class="result-empty">
      <AppEmpty text="搜索失败，请稍后重试" />
      <ElButton @click="runSearch(page)">重试</ElButton>
    </div>
    <AppEmpty v-else-if="records.length === 0" text="没有找到相关记录" />
    <ul v-else class="result-list">
      <li v-for="item in records" :key="`${item.type}-${item.id}`">
        <button type="button" class="result-hit" @click="open(item)">
          <div class="result-line">
            <span class="result-tag" :class="`tag-${item.type}`">{{ typeLabel[item.type] }}</span>
            <span class="result-title">{{ item.title }}</span>
            <span class="result-date">{{ item.date }}</span>
          </div>
          <p class="result-snippet" v-html="highlight(item.snippet, q.trim())"></p>
        </button>
      </li>
    </ul>

    <AppPagination v-if="total > 0" :page="page" :page-size="pageSize" :total="total" @change="onPageChange" />
  </div>
</template>

<style scoped>
.search-page {
  max-width: 760px;
}
.search-bar {
  display: flex;
  gap: 10px;
}
.search-bar .el-input {
  flex: 1;
}
.type-filter {
  margin: 14px 0 4px;
}
.result-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.result-list {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.result-hit {
  width: 100%;
  text-align: left;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 14px 16px;
  cursor: pointer;
  transition: border-color var(--dur) ease;
}
.result-hit:hover {
  border-color: var(--accent);
}
.result-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.result-tag {
  flex: none;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 999px;
  color: #fff;
}
.tag-diary { background: var(--mark-diary); }
.tag-note { background: var(--accent-blue); }
.tag-study { background: var(--mark-study); }
.tag-event { background: var(--mark-event); }
.result-title {
  color: var(--ink);
  font-size: 14.5px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.result-date {
  margin-left: auto;
  flex: none;
  font-size: 12px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.result-snippet {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ink-2);
}
.result-snippet :deep(mark) {
  background: var(--accent-soft);
  color: var(--accent-deep);
  padding: 0 2px;
  border-radius: 2px;
}
</style>