<script setup lang="ts">
import { ElPagination } from 'element-plus'

const props = defineProps<{
  page: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'change', payload: { page: number; pageSize: number }): void
}>()

function onChange(page: number, pageSize: number) {
  emit('change', { page, pageSize })
}
</script>

<template>
  <div v-if="props.total > 0" class="app-pagination">
    <ElPagination
      background
      layout="prev, pager, next, sizes, total"
      :total="props.total"
      :page-size="props.pageSize"
      :current-page="props.page"
      :page-sizes="[10, 20, 50, 100]"
      @update:current-page="(p: number) => onChange(p, props.pageSize)"
      @update:page-size="(s: number) => onChange(1, s)"
    />
  </div>
</template>

<style scoped>
.app-pagination {
  display: flex;
  justify-content: center;
  padding: 18px 0 4px;
}
</style>