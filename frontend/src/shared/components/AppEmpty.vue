<script setup lang="ts">
defineProps<{
  text?: string
}>()
</script>

<template>
  <div class="app-empty" role="status">
    <div class="empty-mark" aria-hidden="true">◦</div>
    <p class="empty-text">{{ text ?? '这里还是一片空白' }}</p>
  </div>
</template>

<style scoped>
.app-empty {
  padding: 48px 16px;
  text-align: center;
  color: var(--muted);
}
.empty-mark {
  font-size: 30px;
  line-height: 1;
  color: var(--line-strong);
  margin-bottom: 8px;
}
.empty-text {
  font-size: 13px;
}
</style>