<script setup lang="ts">
/**
 * Markdown 编辑器：左侧编辑 + 右侧预览（marked 渲染）。
 */
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const previewHtml = computed(() => marked.parse(props.modelValue || '') as string)

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <div class="md-editor">
    <div class="md-pane">
      <div class="pane-head">编辑</div>
      <textarea class="md-textarea" :value="props.modelValue" placeholder="在这里书写 Markdown…" @input="onInput" />
    </div>
    <div class="md-pane">
      <div class="pane-head">预览</div>
      <div class="md-preview" v-html="previewHtml" />
    </div>
  </div>
</template>

<style scoped>
.md-editor {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  background: var(--surface);
  padding: 14px;
}
.md-pane {
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.pane-head {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 8px;
  letter-spacing: 0.04em;
}
.md-textarea {
  width: 100%;
  min-height: 380px;
  resize: vertical;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--bg-soft);
  color: var(--ink);
  font-family: var(--font-sans);
  font-size: 13.5px;
  line-height: 1.7;
  padding: 12px;
  outline: none;
}
.md-textarea:focus {
  border-color: var(--accent);
}
.md-preview {
  min-height: 380px;
  overflow: auto;
  padding: 12px 14px;
  border-radius: var(--radius);
  background: var(--bg-soft);
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink-2);
}
.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3) {
  color: var(--ink);
  margin: 12px 0 6px;
}
.md-preview :deep(h1) { font-size: 21px; }
.md-preview :deep(h2) { font-size: 17px; }
.md-preview :deep(h3) { font-size: 15px; }
.md-preview :deep(p) { margin: 6px 0; }
.md-preview :deep(ul),
.md-preview :deep(ol) { padding-left: 20px; margin: 6px 0; }
.md-preview :deep(blockquote) {
  border-left: 3px solid var(--line-strong);
  color: var(--muted);
  padding-left: 10px;
  margin: 8px 0;
}
.md-preview :deep(code) {
  font-family: Consolas, Monaco, monospace;
  font-size: 12.5px;
  background: var(--accent-soft);
  border-radius: 4px;
  padding: 1px 5px;
}
.md-preview :deep(pre) {
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 10px;
  overflow: auto;
}
.md-preview :deep(pre code) {
  background: none;
  padding: 0;
}
.md-preview :deep(a) { color: var(--accent-deep); }
@media (max-width: 760px) {
  .md-editor {
    grid-template-columns: 1fr;
  }
}
</style>