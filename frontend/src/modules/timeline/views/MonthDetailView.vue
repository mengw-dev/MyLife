<script setup lang="ts">
/**
 * 月份详情：人生时间线 / {年} / {月} —— 完整月历与三类记录标记。
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTimelineStore } from '../stores/timeline'
import MonthCalendar from '../components/MonthCalendar.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import { PATHS } from '@/shared/constants/paths'

const route = useRoute()
const router = useRouter()
const timeline = useTimelineStore()

const year = computed(() => Number(route.params.year))
const month = computed(() => Number(route.params.month))

watch([year, month], ([y, m]) => { if (y && m) timeline.loadMonth(y, m) }, { immediate: true })

const loaded = computed(() => {
  const first = timeline.monthDays[0]?.date
  return first ? first.startsWith(`${year.value}-${String(month.value).padStart(2, '0')}`) : false
})

function shift(delta: number) {
  const d = new Date(year.value, month.value - 1 + delta, 1)
  router.push(PATHS.timelineMonth(d.getFullYear(), d.getMonth() + 1))
}

function openDay(date: string) {
  router.push(PATHS.timelineDay(date))
}
</script>

<template>
  <div class="app-page">
    <nav class="crumbs" aria-label="面包屑">
      <RouterLink :to="PATHS.timeline">人生时间线</RouterLink>
      <span class="sep">/</span>
      <RouterLink :to="PATHS.timelineYear(year)">{{ year }} 年</RouterLink>
      <span class="sep">/</span>
      <span class="current">{{ month }} 月</span>
    </nav>

    <header class="month-head">
      <div>
        <h1 class="month-title">{{ year }} 年 {{ month }} 月</h1>
        <p class="legend">
          <span><i class="lg lg-diary"></i>日记</span>
          <span><i class="lg lg-study"></i>学习</span>
          <span><i class="lg lg-event"></i>重要事件</span>
        </p>
      </div>
      <div class="month-nav">
        <button type="button" @click="shift(-1)">‹ 上月</button>
        <button type="button" @click="shift(1)">下月 ›</button>
      </div>
    </header>

    <AppLoading v-if="timeline.monthLoading || !loaded" text="正在翻日历…" />
    <MonthCalendar v-else :year="year" :month="month" :days="timeline.monthDays" @select="openDay" />
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
  flex-wrap: wrap;
}
.sep { color: var(--line-strong); }
.current { color: var(--ink-2); font-weight: 500; }
.month-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 18px;
}
.month-title {
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 500;
}
.legend {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--muted);
}
.legend span { display: inline-flex; align-items: center; gap: 5px; }
.lg { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.lg-diary { background: var(--mark-diary); }
.lg-study { background: var(--mark-study); }
.lg-event {
  background: none;
  border: 2px solid var(--mark-event);
  border-radius: 2px;
  width: 7px;
  height: 7px;
}
.month-nav {
  display: flex;
  gap: 8px;
}
.month-nav button {
  background: none;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 5px 14px;
  color: var(--ink-2);
  font-size: 12.5px;
}
.month-nav button:hover { border-color: var(--accent); color: var(--accent-deep); }
</style>