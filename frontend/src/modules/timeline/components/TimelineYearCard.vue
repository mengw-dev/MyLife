<script setup lang="ts">
/**
 * 年份节点：时间线上的一个独立单元（数字档案馆式的年份条目）。
 */
import AppImage from '@/shared/components/AppImage.vue'
import { mediaUrl } from '@/shared/utils/media'
import type { TimelineYearSummary } from '../types/timeline'

defineProps<{
  year: TimelineYearSummary
}>()

const emit = defineEmits<{
  (e: 'select', year: number): void
}>()
</script>

<template>
  <article
    class="year-card"
    :class="{ empty: !year.hasRecords }"
    role="button"
    tabindex="0"
    @click="emit('select', year.year)"
    @keydown.enter="emit('select', year.year)"
  >
    <div class="year-rail" aria-hidden="true">
      <span class="rail-dot" :class="{ dim: !year.hasRecords }"></span>
    </div>

    <div class="year-num">{{ year.year }}</div>

    <div class="year-body">
      <h2 class="year-title">{{ year.title || (year.hasRecords ? `${year.year} 年` : '空白的一年') }}</h2>
      <p v-if="year.summary" class="year-summary">{{ year.summary }}</p>
      <div v-if="year.hasRecords" class="year-meta">
        <span class="meta-item"><i class="dot dot-diary"></i>日记 {{ year.diaryCount }}</span>
        <span class="meta-item"><i class="dot dot-study"></i>学习 {{ year.studyCount }}</span>
        <span class="meta-item"><i class="dot dot-event"></i>事件 {{ year.eventCount }}</span>
      </div>
      <span v-else class="year-empty-tip">尚未写下任何记录</span>
    </div>

    <div v-if="year.coverMediaId" class="year-cover">
      <AppImage :src="mediaUrl(year.coverMediaId)" alt="年度代表照片" ratio="16 / 10" />
    </div>
  </article>
</template>

<style scoped>
.year-card {
  position: relative;
  display: grid;
  grid-template-columns: 18px 128px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
  padding: 26px 4px 34px;
  cursor: pointer;
  border-radius: var(--radius);
  transition: background var(--dur) ease;
}
.year-card:hover {
  background: rgba(255, 255, 255, 0.55);
}
.year-card.empty {
  opacity: 0.55;
}
.year-rail {
  position: relative;
  align-self: stretch;
}
.year-rail::before {
  content: '';
  position: absolute;
  left: 7px;
  top: -26px;
  bottom: -34px;
  width: 1px;
  background: var(--line-strong);
}
.year-card:first-child .year-rail::before {
  top: 8px;
}
.year-card:last-child .year-rail::before {
  bottom: auto;
  height: 100%;
}
.rail-dot {
  position: absolute;
  left: 4px;
  top: 10px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 0 3px var(--bg);
}
.rail-dot.dim {
  background: var(--line-strong);
}
.year-num {
  font-family: var(--font-serif);
  font-size: 44px;
  line-height: 1.1;
  color: var(--ink);
  letter-spacing: 0.02em;
}
.year-title {
  font-size: 17px;
  margin-top: 6px;
}
.year-summary {
  margin-top: 6px;
  color: var(--ink-2);
  font-size: 13.5px;
  max-width: 46em;
}
.year-meta {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: var(--muted);
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}
.dot-diary { background: var(--mark-diary); }
.dot-study { background: var(--mark-study); }
.dot-event { background: var(--mark-event); }
.year-empty-tip {
  display: inline-block;
  margin-top: 10px;
  font-size: 12px;
  color: var(--muted);
  border: 1px dashed var(--line-strong);
  border-radius: 999px;
  padding: 2px 10px;
}
.year-cover {
  width: 150px;
  align-self: center;
}
@media (max-width: 760px) {
  .year-card {
    grid-template-columns: 14px 76px minmax(0, 1fr);
  }
  .year-num { font-size: 32px; }
  .year-cover { display: none; }
}
</style>