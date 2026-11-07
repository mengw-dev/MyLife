<script setup lang="ts">
/**
 * 应用根：按时间线层级深度选择过渡方向（进入→向内深入，返回→向外展开）。
 */
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const transition = ref('fade')

/** 依据路径段推断时间线层级深度（overview=1 / year=2 / month=3 / day=3） */
function timelineDepth(path: string): number {
  const segs = path.split('/').filter(Boolean)
  if (segs[0] !== 'timeline') return 0
  if (segs.includes('year') && segs.includes('month')) return 3
  if (segs.includes('year') || segs[1] === 'day') return 2
  return 1
}

watch(
  () => route.fullPath,
  (to, from) => {
    const td = timelineDepth(to)
    const fd = timelineDepth(from)
    if (td > fd) transition.value = 'zoom-in'
    else if (td < fd) transition.value = 'zoom-out'
    else transition.value = 'fade'
  },
)
</script>

<template>
  <RouterView v-slot="{ Component, route: r }">
    <Transition :name="transition" mode="out-in" appear>
      <component :is="Component" :key="r.path" />
    </Transition>
  </RouterView>
</template>