<script setup lang="ts">
import { ElDialog } from 'element-plus'

defineProps<{
  visible: boolean
  title: string
  width?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()
</script>

<template>
  <ElDialog
    :model-value="visible"
    :title="title"
    :width="width ?? '560px'"
    align-center
    destroy-on-close
    @update:model-value="(v: boolean) => !v && emit('close')"
  >
    <slot />
    <template #footer>
      <slot name="footer" />
    </template>
  </ElDialog>
</template>