<script setup lang="ts">
/**
 * 月份日历：星期排列正确，日记/学习/事件三种标记，事件更醒目，支持左右切换。
 */
import { computed } from 'vue'
import { buildMonthGrid } from '@/shared/utils/date'
import type { TimelineDayStat } from '../types/timeline'

const props = defineProps<{
  year: number
  month: number
  days: TimelineDayStat[]
}>()

const emit = defineEmits<{
  (e: 'select', date: string): void
}>()

const WEEK = ['日', '一', '二', '三', '四', '五', '六']

const stats = computed(() => {
  const map = new Map<string, TimelineDayStat>()
  props.days.forEach((d) => map.set(d.date, d))
  return map
})

const cells = computed(() => buildMonthGrid(props.year, props.month))

function marks(date: string | null): TimelineDayStat | undefined {
  return date ? stats.value.get(date) : undefined
}
</script>

<template>
  <div class="month-calendar" role="grid" aria-label="月份日历">
    <div class="week-head">
      <span v-for="w in WEEK" :key="w" class="week-cell">周{{ w }}</span>
    </div>
    <div class="day-grid">
      <button
        v-for="cell in cells"
        :key="`${cell.date}-${cell.day}`"
        type="button"
        class="day-cell"
        :class="{ outside: !cell.inMonth, empty: !marks(cell.date) }"
        :disabled="!cell.date"
        @click="cell.date && emit('select', cell.date)"
      >
        <span class="day-num">{{ cell.day }}</span>
        <span v-if="cell.date && marks(cell.date)" class="day-marks">
          <i v-if="marks(cell.date)!.diaryCount" class="mk mk-diary" :aria-label="`日记 ${marks(cell.date)!.diaryCount} 篇`"></i>
          <i v-if="marks(cell.date)!.studyCount" class="mk mk-study" :aria-label="`学习 ${marks(cell.date)!.studyCount} 条`"></i>
          <i v-if="marks(cell.date)!.eventCount" class="mk mk-event" :aria-label="`事件 ${marks(cell.date)!.eventCount} 件`"></i>
        </span>
        <span v-else class="day-marks placeholder"></span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.month-calendar {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  padding: 16px;
  box-shadow: var(--shadow-card);
}
.week-head,
.day-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}
.week-cell {
  text-align: center;
  font-size: 12px;
  color: var(--muted);
  padding: 6px 0 10px;
}
.day-cell {
  position: relative;
  background: none;
  border: none;
  border-radius: var(--radius);
  min-height: 74px;
  padding: 6px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  transition: background var(--dur) ease;
}
.day-cell:hover:not(.outside) {
  background: var(--bg-soft);
}
.day-cell.outside {
  opacity: 0.35;
}
.day-num {
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--ink);
}
.day-marks {
  display: flex;
  gap: 3px;
  height: 6px;
  align-items: center;
}
.day-marks.placeholder {
  visibility: hidden;
}
.mk {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.mk-diary { background: var(--mark-diary); }
.mk-study { background: var(--mark-study); }
.mk-event {
  background: none;
  width: 8px;
  height: 8px;
  border: 2px solid var(--mark-event);
  border-radius: 2px;
}
@media (max-width: 640px) {
  .day-cell { min-height: 52px; }
}
</style>