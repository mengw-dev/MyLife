<script setup lang="ts">
/**
 * 管理中心右侧时间线预览（DEV-NOTES §5）：
 * 复用 timeline store 的统一聚合数据；年/月/日三级切换；
 * 叠加 preview store 的未保存草稿（虚线 +「预览」标签），并保留编辑中的原始记录位置。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useTimelineStore } from '@/modules/timeline/stores/timeline'
import { usePreviewStore } from '@/stores/preview'
import { buildMonthGrid, monthNameCn, parseDateKey } from '@/shared/utils/date'
import type { TimelineDayStat } from '@/modules/timeline/types/timeline'

function parseDate(date: string): Date {
  return parseDateKey(date)
}

const props = withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })

const emit = defineEmits<{ (e: 'open-full', date?: string): void }>()

const timeline = useTimelineStore()
const preview = usePreviewStore()

type Zoom = 'year' | 'month' | 'day'
const zoom = ref<Zoom>('month')
const anchor = ref(new Date())

const currentYm = computed(() => `${anchor.value.getFullYear()}-${String(anchor.value.getMonth() + 1).padStart(2, '0')}`)

const monthStats = computed(() => {
  const map = new Map<string, TimelineDayStat>()
  timeline.monthDays.forEach((d) => map.set(d.date, d))
  return map
})

onMounted(async () => {
  if (!timeline.years.length) await timeline.loadYears()
  await syncMonth()
})

watch(currentYm, async () => { await syncMonth() })

async function syncMonth() {
  await timeline.loadMonth(anchor.value.getFullYear(), anchor.value.getMonth() + 1)
}

function switchMonth(delta: number) {
  anchor.value = new Date(anchor.value.getFullYear(), anchor.value.getMonth() + delta, 1)
}

/** 草稿落在某日的标记数（预览 +1） */
function draftCountFor(date: string | null): { diary: number; study: number; event: number; previewed: boolean } {
  const base = date ? monthStats.value.get(date) : undefined
  const draft = preview.draft
  const isTarget = draft?.kind !== 'stage' && draft?.date === date
  const isOrigin = draft?.mode === 'edit' && draft.originDate === date
  return {
    diary: (base?.diaryCount ?? 0) + (isTarget && draft?.kind === 'diary' ? 1 : 0),
    study: (base?.studyCount ?? 0) + (isTarget && draft?.kind === 'study' ? 1 : 0),
    event: (base?.eventCount ?? 0) + (isTarget && draft?.kind === 'event' ? 1 : 0),
    previewed: Boolean(isTarget || isOrigin),
  }
}

const cells = computed(() => buildMonthGrid(anchor.value.getFullYear(), anchor.value.getMonth() + 1))

const stagedYears = computed(() => {
  const d = preview.draft
  if (d?.kind === 'stage' && d.startDate) {
    return [Number(d.startDate.slice(0, 4))]
  }
  return []
})
</script>

<template>
  <div class="timeline-preview" :class="{ compact: props.compact }">
    <div class="zoom-switch" role="tablist" aria-label="预览缩放级别">
      <button v-for="z in (['year', 'month', 'day'] as Zoom[])" :key="z" type="button"
        class="zoom-btn" :class="{ active: zoom === z }" role="tab" @click="zoom = z">
        {{ z === 'year' ? '年份' : z === 'month' ? '月份' : '日期' }}
      </button>
    </div>

    <!-- 年份视图 -->
    <div v-if="zoom === 'year'" class="preview-years">
      <button v-for="y in timeline.years" :key="y.year" type="button" class="preview-year-row"
        :class="{ draft: stagedYears.includes(y.year) }" @click="anchor = new Date(y.year, 0, 1); zoom = 'month'">
        <span class="pv-year">{{ y.year }}</span>
        <span class="pv-year-title">{{ y.title || (y.hasRecords ? '—' : '空白') }}</span>
        <span class="pv-count">{{ y.diaryCount + y.studyCount + y.eventCount }}</span>
      </button>
      <p v-if="preview.draft?.kind === 'stage'" class="stage-hint">阶段草稿：{{ preview.draft.startDate }} 起</p>
    </div>

    <!-- 月份视图 -->
    <div v-else-if="zoom === 'month'" class="preview-month">
      <div class="month-nav">
        <button type="button" aria-label="上个月" @click="switchMonth(-1)">‹</button>
        <span class="month-label">{{ anchor.getFullYear() }} 年 {{ monthNameCn(anchor.getMonth() + 1) }}</span>
        <button type="button" aria-label="下个月" @click="switchMonth(1)">›</button>
      </div>
      <div class="mini-cal">
        <div class="mini-week"><span v-for="w in ['日','一','二','三','四','五','六']" :key="w">{{ w }}</span></div>
        <div class="mini-grid">
          <button v-for="c in cells" :key="`${c.date}`" type="button" class="mini-cell"
            :class="{ outside: !c.inMonth, previewed: c.date && draftCountFor(c.date).previewed }"
            @click="c.date && (zoom = 'day'); c.date && (anchor = parseDate(c.date))">
            <span class="mini-num">{{ c.day }}</span>
            <span v-if="c.date" class="mini-marks">
              <i v-if="draftCountFor(c.date).diary" class="mk mk-diary"></i>
              <i v-if="draftCountFor(c.date).study" class="mk mk-study"></i>
              <i v-if="draftCountFor(c.date).event" class="mk mk-event"></i>
            </span>
          </button>
        </div>
      </div>
      <p v-if="preview.hasDraft" class="draft-hint">
        <span v-if="preview.saving">保存中…</span>
        <span v-else>预览：未保存的改动以虚线标示</span>
      </p>
    </div>

    <!-- 日期视图 -->
    <div v-else class="preview-day">
      <div class="month-nav">
        <button type="button" aria-label="前一天" @click="anchor = new Date(anchor.getTime() - 86400000)">‹</button>
        <span class="month-label">{{ anchor.toLocaleDateString('zh-CN') }}</span>
        <button type="button" aria-label="后一天" @click="anchor = new Date(anchor.getTime() + 86400000)">›</button>
      </div>
      <p class="day-placeholder">点击「查看完整时间线」浏览当日详情</p>
    </div>

    <button type="button" class="open-full" @click="emit('open-full')">查看完整时间线 →</button>
  </div>
</template>

<style scoped>
.timeline-preview {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
}
.zoom-switch {
  display: flex;
  gap: 4px;
  background: var(--bg-soft);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 3px;
}
.zoom-btn {
  flex: 1;
  background: none;
  border: none;
  border-radius: 999px;
  padding: 5px 0;
  font-size: 12.5px;
  color: var(--muted);
}
.zoom-btn.active {
  background: var(--surface);
  color: var(--accent-deep);
  font-weight: 600;
  box-shadow: var(--shadow-panel);
}
.preview-years {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 320px;
  overflow: auto;
}
.preview-year-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  text-align: left;
  background: none;
  border: none;
  border-bottom: 1px solid var(--line);
  padding: 8px 6px;
  color: var(--ink-2);
  border-radius: var(--radius);
}
.preview-year-row:hover { background: var(--bg-soft); }
.preview-year-row.draft { border: 1px dashed var(--warning); }
.pv-year { font-family: var(--font-serif); font-size: 16px; color: var(--ink); }
.pv-year-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.pv-count { color: var(--muted); font-size: 12px; }
.stage-hint, .draft-hint {
  font-size: 11.5px;
  color: var(--warning);
  margin: 4px 2px;
}
.month-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.month-nav button {
  background: none;
  border: none;
  font-size: 16px;
  color: var(--accent-deep);
  padding: 2px 8px;
}
.month-label { font-size: 12.5px; font-weight: 600; color: var(--ink-2); }
.mini-week, .mini-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 2px;
}
.mini-week span {
  text-align: center;
  font-size: 11px;
  color: var(--muted);
  padding: 3px 0;
}
.mini-cell {
  background: none;
  border: 1px solid transparent;
  border-radius: 6px;
  min-height: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 3px 0;
  color: var(--ink-2);
}
.mini-cell:hover { background: var(--bg-soft); }
.mini-cell.outside { opacity: 0.3; }
.mini-cell.previewed {
  border: 1px dashed var(--warning);
  background: rgba(169, 126, 63, 0.06);
}
.mini-num { font-size: 12px; }
.mini-marks { display: flex; gap: 2px; }
.mk { width: 4px; height: 4px; border-radius: 50%; }
.mk-diary { background: var(--mark-diary); }
.mk-study { background: var(--mark-study); }
.mk-event {
  background: none;
  width: 5px;
  height: 5px;
  border: 1.5px solid var(--mark-event);
}
.day-placeholder {
  color: var(--muted);
  font-size: 12px;
  text-align: center;
  padding: 18px 0;
}
.open-full {
  background: none;
  border: none;
  color: var(--accent-deep);
  font-size: 12.5px;
  padding: 6px;
  border-top: 1px solid var(--line);
}
</style>