<script setup lang="ts">
defineProps<{
  text?: string
}>()
</script>

<template>
  <div class="app-loading" role="status" aria-live="polite">
    <span class="loading-dot" aria-hidden="true"></span>
    <span>{{ text ?? '正在加载…' }}</span>
  </div>
</template>

<style scoped>
.app-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 16px;
  color: var(--muted);
  font-size: 13px;
}
.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse 1s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 0.25; }
  50% { opacity: 1; }
}
</style>