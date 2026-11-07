<script setup lang="ts">
/**
 * 日记列表：分页 + 关键词/日期区间/阶段/标签筛选 + 新建/编辑/删除。
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElDatePicker, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppPagination from '@/shared/components/AppPagination.vue'
import { PATHS } from '@/shared/constants/paths'
import { diaryApi } from '../api/diary.api'
import { lifeStageApi } from '@/modules/life-stage/api/life-stage.api'
import type { Diary, LifeStage } from '@/shared/types/entities'

const router = useRouter()

const loading = ref(false)
const error = ref(false)
const list = ref<Diary[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const stageOptions = ref<LifeStage[]>([])

const filters = reactive({
  keyword: '',
  range: null as string[] | null,
  stageId: '',
  tag: '',
})

async function load() {
  loading.value = true
  error.value = false
  try {
    const res = await diaryApi.list({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      from: filters.range?.[0],
      to: filters.range?.[1],
      stageId: filters.stageId || undefined,
      tag: filters.tag || undefined,
    })
    list.value = res.records
    total.value = res.total
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.range = null
  filters.stageId = ''
  filters.tag = ''
  page.value = 1
  load()
}

function onPageChange(payload: { page: number; pageSize: number }) {
  page.value = payload.page
  pageSize.value = payload.pageSize
  load()
}

function newDiary() {
  router.push(PATHS.diaryNew)
}

function editDiary(d: Diary) {
  router.push(PATHS.diaryEdit(d.id))
}

async function removeDiary(d: Diary) {
  try {
    await ElMessageBox.confirm(`确定删除「${d.title}」吗？`, '删除日记', { type: 'warning' })
  } catch {
    return
  }
  try {
    await diaryApi.remove(d.id)
    ElMessage.success('已删除')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  }
}

function stageName(id: string | null): string {
  if (!id) return ''
  return stageOptions.value.find((s) => s.id === id)?.name ?? '未分类'
}

onMounted(async () => {
  stageOptions.value = await lifeStageApi.list().catch(() => [] as LifeStage[])
  load()
})
</script>

<template>
  <div class="list-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">日记管理</h1>
        <p class="page-sub">共 {{ total }} 篇日记</p>
      </div>
      <ElButton type="primary" @click="newDiary">新建日记</ElButton>
    </header>

    <div class="filter-bar">
      <ElInput v-model="filters.keyword" placeholder="搜索标题 / 正文" clearable style="width: 180px" @keyup.enter="search" @clear="search" />
      <ElDatePicker
        v-model="filters.range"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
      <ElSelect v-model="filters.stageId" placeholder="阶段" clearable style="width: 150px">
        <ElOption v-for="s in stageOptions" :key="s.id" :label="s.name" :value="s.id" />
      </ElSelect>
      <ElInput v-model="filters.tag" placeholder="标签" clearable style="width: 120px" @keyup.enter="search" @clear="search" />
      <ElButton @click="search">搜索</ElButton>
      <ElButton @click="reset">重置</ElButton>
    </div>

    <AppLoading v-if="loading" text="正在加载日记…" />
    <div v-else-if="error" class="state-error" role="status">
      <p>加载失败，请稍后重试</p>
      <ElButton @click="load">重试</ElButton>
    </div>
    <AppEmpty v-else-if="!list.length" text="还没有日记，写一篇记录当下吧" />
    <div v-else class="diary-list">
      <article v-for="d in list" :key="d.id" class="diary-card">
        <div class="card-main">
          <h2 class="card-title">{{ d.title }}</h2>
          <p class="card-meta">
            <span class="date">{{ d.recordDate }}</span>
            <span v-if="d.stageId" class="stage">{{ stageName(d.stageId) }}</span>
          </p>
          <p class="card-excerpt">{{ d.content }}</p>
          <p v-if="d.tagNames?.length" class="card-tags">
            <span v-for="t in d.tagNames" :key="t" class="tag"># {{ t }}</span>
          </p>
        </div>
        <div class="card-actions">
          <ElButton size="small" @click="editDiary(d)">编辑</ElButton>
          <ElButton size="small" type="danger" @click="removeDiary(d)">删除</ElButton>
        </div>
      </article>
    </div>

    <AppPagination :page="page" :page-size="pageSize" :total="total" @change="onPageChange" />
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
.filter-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin: 18px 0;
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
.diary-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.diary-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
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
.card-meta .stage {
  color: var(--accent-deep);
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
  color: var(--accent-blue);
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