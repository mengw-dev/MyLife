<script setup lang="ts">
/**
 * 年度详情：人生时间线 / {年} —— 年度资料 + 全年事件 + 12 个月概览。
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTimelineStore } from '../stores/timeline'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppImage from '@/shared/components/AppImage.vue'
import { mediaUrl } from '@/shared/utils/media'
import { PATHS } from '@/shared/constants/paths'

const route = useRoute()
const router = useRouter()
const timeline = useTimelineStore()

const year = computed(() => Number(route.params.year))

watch(year, (y) => { if (y) timeline.loadYear(y) }, { immediate: true })

function openMonth(month: number) {
  router.push(PATHS.timelineMonth(year.value, month))
}

function go(yearOffset: number) {
  router.push(PATHS.timelineYear(year.value + yearOffset))
}
</script>

<template>
  <div class="app-page">
    <nav class="crumbs" aria-label="面包屑">
      <RouterLink :to="PATHS.timeline">人生时间线</RouterLink>
      <span class="sep">/</span>
      <span class="current">{{ year }} 年</span>
    </nav>

    <AppLoading v-if="timeline.yearLoading || timeline.yearDetail?.year !== year" text="正在打开这一年…" />

    <template v-else-if="timeline.yearDetail">
      <header class="year-head">
        <div class="year-head-text">
          <h1 class="year-big">{{ year }}</h1>
          <h2 class="year-title">{{ timeline.yearDetail.title || `${year} 年` }}</h2>
          <p v-if="timeline.yearDetail.summary" class="year-summary">{{ timeline.yearDetail.summary }}</p>
        </div>
        <div v-if="timeline.yearDetail.coverMediaId" class="year-cover">
          <AppImage :src="mediaUrl(timeline.yearDetail.coverMediaId)" alt="年度代表照片" ratio="16 / 10" />
        </div>
      </header>

      <div class="year-nav">
        <button type="button" @click="go(-1)">← {{ year - 1 }} 年</button>
        <button type="button" @click="go(1)">{{ year + 1 }} 年 →</button>
      </div>

      <section v-if="timeline.yearDetail.events.length" class="year-events">
        <h3 class="section-title">这一年的大事</h3>
        <ul>
          <li v-for="ev in timeline.yearDetail.events" :key="ev.id" class="event-row">
            <span class="event-date">{{ ev.eventDate.slice(5) }}</span>
            <span class="event-title">{{ ev.title }}</span>
          </li>
        </ul>
      </section>

      <section class="year-months">
        <h3 class="section-title">月份</h3>
        <div class="month-grid">
          <button v-for="m in timeline.yearDetail.months" :key="m.month" type="button"
            class="month-tile" :class="{ empty: !m.hasRecords }"
            @click="openMonth(m.month)">
            <span class="month-name">{{ m.month }} 月</span>
            <span class="month-counts">
              <span v-if="m.diaryCount">日记 {{ m.diaryCount }}</span>
              <span v-if="m.studyCount">学习 {{ m.studyCount }}</span>
              <span v-if="m.eventCount">事件 {{ m.eventCount }}</span>
              <span v-if="!m.hasRecords" class="no-record">空白</span>
            </span>
          </button>
        </div>
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
.crumbs a:hover { color: var(--accent-deep); }
.sep { color: var(--line-strong); }
.current { color: var(--ink-2); font-weight: 500; }
.year-head {
  display: flex;
  gap: 28px;
  align-items: center;
  justify-content: space-between;
}
.year-big {
  font-family: var(--font-serif);
  font-size: 76px;
  font-weight: 400;
  line-height: 1;
  color: var(--ink);
}
.year-title {
  font-size: 20px;
  margin-top: 10px;
}
.year-summary {
  margin-top: 6px;
  color: var(--ink-2);
  font-size: 14px;
  max-width: 42em;
}
.year-cover { width: 240px; flex: none; }
.year-nav {
  display: flex;
  justify-content: space-between;
  margin: 22px 0 6px;
}
.year-nav button {
  background: none;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 5px 14px;
  color: var(--ink-2);
  font-size: 12.5px;
  transition: border-color var(--dur) ease;
}
.year-nav button:hover { border-color: var(--accent); color: var(--accent-deep); }
.section-title {
  font-size: 15px;
  margin: 26px 0 12px;
  color: var(--ink-2);
  letter-spacing: 0.04em;
}
.year-events ul {
  border-left: 1px solid var(--line-strong);
  padding-left: 16px;
}
.event-row {
  display: flex;
  gap: 12px;
  padding: 5px 0;
  font-size: 13.5px;
}
.event-date {
  color: var(--muted);
  font-family: var(--font-serif);
  width: 52px;
  flex: none;
}
.month-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.month-tile {
  text-align: left;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 14px;
  box-shadow: var(--shadow-card);
  transition: transform var(--dur) ease, border-color var(--dur) ease;
}
.month-tile:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
}
.month-tile.empty { opacity: 0.55; }
.month-name {
  font-size: 15px;
  font-weight: 600;
}
.month-counts {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  font-size: 12px;
  color: var(--muted);
}
.no-record { border: 1px dashed var(--line-strong); border-radius: 999px; padding: 0 8px; }
@media (max-width: 900px) {
  .month-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .year-big { font-size: 56px; }
  .year-cover { display: none; }
}
</style>