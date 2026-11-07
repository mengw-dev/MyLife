/**
 * 知识库接口 Mock（分类树 + 笔记 CRUD + 学习记录关联）。
 */
import { registerMock, ok, okPage, mockFail, paginate } from '@/shared/api/mock/registry'
import { mockDb, nextId } from '@/shared/api/mock/db'
import { requireMockUser } from '@/shared/api/mock/session'
import type { KnowledgeCategory, KnowledgeNote } from '@/shared/types/entities'

/** 由扁平数组按 sortOrder 构建分类树（重建 children） */
function buildTree(): KnowledgeCategory[] {
  const flat = mockDb.categories.map((c) => ({ ...c, children: [] as KnowledgeCategory[] }))
  const map = new Map(flat.map((c) => [c.id, c]))
  const roots: KnowledgeCategory[] = []
  for (const c of flat) {
    if (c.parentId && map.has(c.parentId)) map.get(c.parentId)!.children.push(c)
    else roots.push(c)
  }
  const sort = (arr: KnowledgeCategory[]) => {
    arr.sort((a, b) => a.sortOrder - b.sortOrder)
    arr.forEach((n) => sort(n.children))
  }
  sort(roots)
  return roots
}

/** candidate 是否为 root 自身或其子孙（向下递归） */
function isSelfOrDescendant(rootId: string, candidateId: string): boolean {
  if (rootId === candidateId) return true
  return mockDb.categories
    .filter((c) => c.parentId === rootId)
    .some((c) => isSelfOrDescendant(c.id, candidateId))
}

export function registerKnowledgeMock(): void {
  // ---- 分类 ----
  registerMock('GET', '/knowledge/categories', (_p, _b, _q, cfg) => {
    requireMockUser(cfg)
    return ok(buildTree())
  })

  registerMock('POST', '/knowledge/categories', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const { name, parentId, sortOrder } = (body ?? {}) as { name?: string; parentId?: string | null; sortOrder?: number }
    if (!name?.trim()) {
      return { status: 400, body: { code: 40001, message: '分类名称不能为空', data: null } }
    }
    const siblings = mockDb.categories.filter((c) => (c.parentId ?? null) === (parentId ?? null))
    const category: KnowledgeCategory = {
      id: nextId(),
      name: name.trim(),
      parentId: parentId ?? null,
      sortOrder: sortOrder ?? siblings.length + 1,
      children: [],
    }
    mockDb.categories.push(category)
    return ok({ id: category.id }, 201)
  })

  registerMock('PUT', '/knowledge/categories/:id', (p, body, _q, cfg) => {
    requireMockUser(cfg)
    const category = mockDb.categories.find((c) => c.id === p.id)
    if (!category) throw mockFail(cfg, 404, 40401, '分类不存在')
    const { name, parentId, sortOrder } = (body ?? {}) as { name?: string; parentId?: string | null; sortOrder?: number }
    if (parentId != null && parentId !== '' && isSelfOrDescendant(p.id, parentId)) {
      throw mockFail(cfg, 400, 40001, '父分类不能是自身或其子孙分类')
    }
    if (name !== undefined) category.name = name.trim() || category.name
    if (parentId !== undefined) category.parentId = parentId === '' ? null : parentId ?? null
    if (sortOrder !== undefined) category.sortOrder = sortOrder
    return ok(null)
  })

  registerMock('DELETE', '/knowledge/categories/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    if (mockDb.categories.some((c) => c.parentId === p.id)) {
      throw mockFail(cfg, 409, 40901, '存在子分类，无法删除')
    }
    if (mockDb.notes.some((n) => n.categoryId === p.id)) {
      throw mockFail(cfg, 409, 40901, '分类下存在知识笔记，无法删除')
    }
    mockDb.categories = mockDb.categories.filter((c) => c.id !== p.id)
    return ok(null)
  })

  // ---- 笔记 ----
  registerMock('GET', '/knowledge/notes', (_p, _b, q, cfg) => {
    requireMockUser(cfg)
    const keyword = (q.keyword ?? '').trim().toLowerCase()
    const categoryId = q.categoryId ?? ''
    const tag = q.tag ?? ''
    const page = Number(q.page ?? 1)
    const pageSize = Number(q.pageSize ?? 10)
    let list = [...mockDb.notes]
    if (keyword) {
      list = list.filter((n) => n.title.toLowerCase().includes(keyword) || n.contentMarkdown.toLowerCase().includes(keyword))
    }
    if (categoryId) list = list.filter((n) => n.categoryId === categoryId)
    if (tag) list = list.filter((n) => n.tagNames.includes(tag))
    const paged = paginate(list, page, pageSize)
    return okPage(paged.records, paged.page, paged.pageSize, paged.total)
  })

  registerMock('POST', '/knowledge/notes', (_p, body, _q, cfg) => {
    requireMockUser(cfg)
    const { title, contentMarkdown, categoryId, tagNames, mediaIds } = (body ?? {}) as {
      title?: string; contentMarkdown?: string; categoryId?: string; tagNames?: string[]; mediaIds?: string[]
    }
    if (!title?.trim()) {
      return { status: 400, body: { code: 40001, message: '笔记标题不能为空', data: null } }
    }
    const now = new Date().toISOString()
    const note: KnowledgeNote = {
      id: nextId(),
      title: title.trim(),
      contentMarkdown: contentMarkdown ?? '',
      categoryId: categoryId ?? '',
      tagNames: tagNames ?? [],
      mediaIds: mediaIds ?? [],
      createdAt: now,
      updatedAt: now,
    }
    mockDb.notes.push(note)
    return ok({ id: note.id }, 201)
  })

  registerMock('GET', '/knowledge/notes/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    const note = mockDb.notes.find((n) => n.id === p.id)
    if (!note) throw mockFail(cfg, 404, 40401, '笔记不存在')
    return ok(note)
  })

  registerMock('PUT', '/knowledge/notes/:id', (p, body, _q, cfg) => {
    requireMockUser(cfg)
    const note = mockDb.notes.find((n) => n.id === p.id)
    if (!note) throw mockFail(cfg, 404, 40401, '笔记不存在')
    const { title, contentMarkdown, categoryId, tagNames, mediaIds } = (body ?? {}) as {
      title?: string; contentMarkdown?: string; categoryId?: string; tagNames?: string[]; mediaIds?: string[]
    }
    if (title !== undefined) note.title = title.trim() || note.title
    if (contentMarkdown !== undefined) note.contentMarkdown = contentMarkdown
    if (categoryId !== undefined) note.categoryId = categoryId
    if (tagNames !== undefined) note.tagNames = tagNames
    if (mediaIds !== undefined) note.mediaIds = mediaIds
    note.updatedAt = new Date().toISOString()
    return ok(null)
  })

  registerMock('DELETE', '/knowledge/notes/:id', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    mockDb.notes = mockDb.notes.filter((n) => n.id !== p.id)
    // 关联学习记录保留 noteTitleSnapshot，仅解除关联
    mockDb.records.forEach((r) => {
      if (r.noteId === p.id) r.noteId = null
    })
    return ok(null)
  })

  registerMock('GET', '/knowledge/notes/:id/study-records', (p, _b, _q, cfg) => {
    requireMockUser(cfg)
    return ok(mockDb.records.filter((r) => r.noteId === p.id))
  })
}