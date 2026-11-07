/**
 * 分类树工具：扁平化为下拉选项（可选排除某节点及其子树）。
 */
import type { KnowledgeCategory } from '@/shared/types/entities'

export interface FlatCategory {
  id: string
  name: string
  depth: number
}

/** 深度遍历扁平化；excludeId 命中时跳过该节点整棵子树（用于编辑时排除自身） */
export function flatCategories(nodes: KnowledgeCategory[], excludeId?: string | null, depth = 0): FlatCategory[] {
  const out: FlatCategory[] = []
  for (const n of nodes) {
    if (n.id === excludeId) continue
    out.push({ id: n.id, name: n.name, depth })
    out.push(...flatCategories(n.children, excludeId, depth + 1))
  }
  return out
}