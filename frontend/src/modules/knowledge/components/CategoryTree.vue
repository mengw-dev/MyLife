<script setup lang="ts">
/**
 * 分类树：轻量树形展示（el-tree），节点操作事件冒泡给父级。
 */
import { ElTree } from 'element-plus'
import type { KnowledgeCategory } from '@/shared/types/entities'

const props = defineProps<{
  categories: KnowledgeCategory[]
}>()

const emit = defineEmits<{
  (e: 'select', category: KnowledgeCategory): void
  (e: 'edit', category: KnowledgeCategory): void
  (e: 'new', parent: KnowledgeCategory): void
  (e: 'delete', category: KnowledgeCategory): void
}>()

const treeProps = { label: 'name', children: 'children' }

function onSelect(node: KnowledgeCategory) {
  emit('select', node)
}
function onEdit(node: KnowledgeCategory) {
  emit('edit', node)
}
function onNewChild(node: KnowledgeCategory) {
  emit('new', node)
}
function onDelete(node: KnowledgeCategory) {
  emit('delete', node)
}
</script>

<template>
  <ElTree :data="props.categories" node-key="id" :props="treeProps" :expand-on-click-node="false" default-expand-all>
    <template #default="{ data }">
      <div class="tree-node">
        <button type="button" class="node-label" @click="onSelect(data)">{{ data.name }}</button>
        <span class="node-actions">
          <button type="button" class="node-btn" title="新建子分类" @click="onNewChild(data)">＋</button>
          <button type="button" class="node-btn" title="编辑" @click="onEdit(data)">编辑</button>
          <button type="button" class="node-btn danger" title="删除" @click="onDelete(data)">删除</button>
        </span>
      </div>
    </template>
  </ElTree>
</template>

<style scoped>
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-right: 6px;
}
.node-label {
  background: none;
  border: none;
  color: var(--ink);
  font-size: 13.5px;
  text-align: left;
  padding: 4px 6px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: background var(--dur) ease, color var(--dur) ease;
}
.node-label:hover {
  background: var(--accent-soft);
  color: var(--accent-deep);
}
.node-actions {
  display: inline-flex;
  gap: 2px;
  opacity: 0;
  transition: opacity var(--dur) ease;
}
.tree-node:hover .node-actions,
.tree-node:focus-within .node-actions {
  opacity: 1;
}
.node-btn {
  background: none;
  border: none;
  color: var(--muted);
  font-size: 12px;
  padding: 2px 6px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: color var(--dur) ease;
}
.node-btn:hover {
  color: var(--accent-deep);
}
.node-btn.danger:hover {
  color: var(--danger);
}
</style>