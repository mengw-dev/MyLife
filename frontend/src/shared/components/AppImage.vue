<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  src?: string | null
  alt?: string
  ratio?: string
}>(), {
  src: null,
  alt: '',
  ratio: '4 / 3',
})

const failed = ref(false)

watch(() => props.src, () => { failed.value = false })

function onError() {
  failed.value = true
}
</script>

<template>
  <div class="app-image" :style="{ aspectRatio: ratio }">
    <img
      v-if="src && !failed"
      :src="src"
      :alt="alt"
      loading="lazy"
      @error="onError"
    />
    <div v-else class="image-fallback" aria-label="图片不可用">
      <span aria-hidden="true">▧</span>
    </div>
  </div>
</template>

<style scoped>
.app-image {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  background: var(--bg-soft);
}
img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-strong);
  font-size: 22px;
}
</style>