<script setup lang="ts">
/**
 * 相册网格卡片：缩略图 + 文件名 + 大小，点击回传当前照片。
 */
import AppImage from '@/shared/components/AppImage.vue'
import { mediaUrl } from '@/shared/utils/media'
import type { MediaFile } from '@/shared/types/entities'

const props = defineProps<{ media: MediaFile }>()

const emit = defineEmits<{
  (e: 'select', media: MediaFile): void
}>()

function fmtSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
  <button type="button" class="media-card" @click="emit('select', props.media)">
    <AppImage :src="mediaUrl(props.media.id)" :alt="props.media.originalName" />
    <div class="media-meta">
      <span class="media-name">{{ props.media.originalName }}</span>
      <span class="media-size">{{ fmtSize(props.media.sizeBytes) }}</span>
    </div>
  </button>
</template>

<style scoped>
.media-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 8px;
  text-align: left;
  cursor: pointer;
  transition: border-color var(--dur) ease, box-shadow var(--dur) ease;
}
.media-card:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-panel);
}
.media-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 4px 2px;
}
.media-name {
  font-size: 12.5px;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.media-size {
  font-size: 11px;
  color: var(--muted);
}
</style>