<script setup lang="ts">
/**
 * 年份总览 —— MyLife 的门面：纵向人生时间线。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTimelineStore } from '../stores/timeline'
import TimelineYearCard from '../components/TimelineYearCard.vue'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import { PATHS } from '@/shared/constants/paths'

const router = useRouter()
const timeline = useTimelineStore()

const desc = ref(false)

const years = computed(() => (desc.value ? [...timeline.years].reverse() : timeline.years))

onMounted(() => {
  if (!timeline.years.length) timeline.loadYears()
})

function openYear(year: number) {
  router.push(PATHS.timelineYear(year))
}
</script>

<template>
  <div class="app-page overview-page">
    <header class="overview-head">
      <div>
        <h1 class="overview-title">人生时间线</h1>
        <p class="overview-sub">从这里，回到你人生中的任何一天。</p>
      </div>
      <button type="button" class="sort-btn" @click="desc = !desc">
        {{ desc ? '正序查看' : '倒序查看' }}
      </button>
    </header>

    <AppLoading v-if="timeline.yearsLoading" text="正在翻阅你的人生…" />
    <AppEmpty v-else-if="!timeline.years.length" text="时间线还是空的，去管理中心写下第一篇日记吧" />

    <section v-else class="years-rail" aria-label="年份列表">
      <TimelineYearCard
        v-for="y in years"
        :key="y.year"
        :year="y"
        @select="openYear"
      />
    </section>
  </div>
</template>

<style scoped>
.overview-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin: 8px 0 4px;
}
.overview-title {
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 500;
  letter-spacing: 0.02em;
}
.overview-sub {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13.5px;
}
.sort-btn {
  background: none;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--ink-2);
  font-size: 12.5px;
  padding: 6px 14px;
  transition: border-color var(--dur) ease, color var(--dur) ease;
}
.sort-btn:hover {
  border-color: var(--accent);
  color: var(--accent-deep);
}
.years-rail {
  margin-top: 8px;
}
</style>