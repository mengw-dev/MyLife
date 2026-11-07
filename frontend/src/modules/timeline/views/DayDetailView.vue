<script setup lang="ts">
/**
 * 日期详情：人生时间线 / {日期} —— 今日生活 / 今日学习 / 今日事件。
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTimelineStore } from '../stores/timeline'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import { parseDateKey, toDateKey } from '@/shared/utils/date'
import { PATHS } from '@/shared/constants/paths'

const route = useRoute()
const router = useRouter()
const timeline = useTimelineStore()

const date = computed(() => String(route.params.date))

watch(date, (d) => { if (d) timeline.loadDay(d) }, { immediate: true })

function shiftDay(delta: number) {
  const d = parseDateKey(date.value)
  d.setDate(d.getDate() + delta)
  router.push(PATHS.timelineDay(toDateKey(d)))
}

const totalRecords = computed(() => {
  const day = timeline.day
  if (!day) return 0
  return day.diaries.length + day.studyRecords.length + day.events.length
})

function newDiary() {
  router.push({ path: PATHS.diaryNew, query: { recordDate: date.value } })
}
</script>

<template>
  <div class="app-page">
    <nav class="crumbs" aria-label="面包屑">
      <RouterLink :to="PATHS.timeline">人生时间线</RouterLink>
      <span class="sep">/</span>
      <span class="current">{{ date }}</span>
    </nav>

    <header class="day-head">
      <div>
        <h1 class="day-title">{{ date }}</h1>
        <p class="day-sub">
          {{ timeline.day ? `这一天有 ${totalRecords} 条记录` : '' }}
        </p>
      </div>
      <div class="day-nav">
        <button type="button" @click="shiftDay(-1)">‹ 前一天</button>
        <button v-if="totalRecords === 0" type="button" class="new-diary" @click="newDiary">写点什么</button>
        <button type="button" @click="shiftDay(1)">后一天 ›</button>
      </div>
    </header>

    <AppLoading v-if="timeline.dayLoading || timeline.day?.date !== date" text="正在展开这一天…" />
    <AppEmpty v-else-if="totalRecords === 0" text="这一天没有留下记录，但它依然属于你" />

    <template v-else-if="timeline.day">
      <section class="day-section">
        <h2 class="section-title">今日生活</h2>
        <div v-if="timeline.day.diaries.length" class="record-list">
          <article v-for="d in timeline.day.diaries" :key="d.id" class="diary-item">
            <h3 class="diary-title">{{ d.title }}</h3>
            <p class="diary-body">{{ d.content }}</p>
            <p v-if="d.tagNames?.length" class="diary-tags">
              <span v-for="t in d.tagNames" :key="t" class="tag"># {{ t }}</span>
            </p>
          </article>
        </div>
        <p v-else class="none-tip">今天没有写日记</p>
      </section>

      <section class="day-section">
        <h2 class="section-title">今日学习</h2>
        <div v-if="timeline.day.studyRecords.length" class="record-list">
          <article v-for="r in timeline.day.studyRecords" :key="r.id" class="study-item">
            <h3 class="study-title">{{ r.title || '学习记录' }}</h3>
            <p class="study-body">{{ r.content }}</p>
            <p v-if="r.understanding" class="study-meta"><em>当时理解：</em>{{ r.understanding }}</p>
            <p v-if="r.questions" class="study-meta"><em>待解答：</em>{{ r.questions }}</p>
            <p v-if="r.noteTitleSnapshot" class="study-meta"><em>关联笔记：</em>{{ r.noteTitleSnapshot }}</p>
          </article>
        </div>
        <p v-else class="none-tip">今天没有学习记录</p>
      </section>

      <section class="day-section">
        <h2 class="section-title">今日重要事件</h2>
        <div v-if="timeline.day.events.length" class="record-list">
          <article v-for="ev in timeline.day.events" :key="ev.id" class="event-item">
            <h3 class="event-title">{{ ev.title }}</h3>
            <p v-if="ev.description" class="event-desc">{{ ev.description }}</p>
            <p v-if="ev.tagNames?.length" class="diary-tags">
              <span v-for="t in ev.tagNames" :key="t" class="tag"># {{ t }}</span>
            </p>
          </article>
        </div>
        <p v-else class="none-tip">今天没有重要事件</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
  margin-bottom: 18px;
}
.sep { color: var(--line-strong); }
.current { color: var(--ink-2); font-weight: 500; }
.day-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.day-title {
  font-family: var(--font-serif);
  font-size: 34px;
  font-weight: 500;
}
.day-sub {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}
.day-nav {
  display: flex;
  gap: 8px;
}
.day-nav button {
  background: none;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 5px 14px;
  color: var(--ink-2);
  font-size: 12.5px;
}
.day-nav button:hover { border-color: var(--accent); color: var(--accent-deep); }
.new-diary {
  background: var(--accent-soft) !important;
  color: var(--accent-deep) !important;
  border-color: var(--accent) !important;
}
.day-section {
  margin-top: 30px;
}
.section-title {
  font-size: 14px;
  letter-spacing: 0.08em;
  color: var(--muted);
  border-bottom: 1px solid var(--line);
  padding-bottom: 10px;
}
.record-list {
  display: flex;
  flex-direction: column;
}
.diary-item,
.study-item,
.event-item {
  padding: 14px 2px;
  border-bottom: 1px dashed var(--line);
}
.diary-title, .study-title, .event-title {
  font-size: 15px;
}
.diary-body, .study-body, .event-desc {
  margin-top: 6px;
  color: var(--ink-2);
  font-size: 13.5px;
  white-space: pre-wrap;
}
.study-meta {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--muted);
}
.study-meta em { font-style: normal; color: var(--accent-deep); }
.event-item {
  border-left: 3px solid var(--mark-event);
  padding-left: 12px;
}
.diary-tags {
  margin-top: 8px;
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
.none-tip {
  color: var(--muted);
  font-size: 13px;
  padding: 12px 2px;
}
</style>