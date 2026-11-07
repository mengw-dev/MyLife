/**
 * 路由表：集中管理，页面组件全部懒加载（DEV-NOTES §2）。
 * meta.transition 控制时间线层级过渡方向（见 styles/transitions.css）。
 */
import type { RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import ManageLayout from '@/layouts/ManageLayout.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/modules/auth/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/modules/auth/views/RegisterView.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/',
    redirect: '/timeline',
  },
  // ---- 主时间线 ----
  {
    path: '/timeline',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'timeline',
        component: () => import('@/modules/timeline/views/TimelineOverviewView.vue'),
        meta: { title: '人生时间线', depth: 1 },
      },
      {
        path: 'year/:year',
        name: 'timeline-year',
        component: () => import('@/modules/timeline/views/YearDetailView.vue'),
        meta: { title: '年度详情', depth: 2, transition: 'zoom-in' },
      },
      {
        path: 'year/:year/month/:month',
        name: 'timeline-month',
        component: () => import('@/modules/timeline/views/MonthDetailView.vue'),
        meta: { title: '月份详情', depth: 3, transition: 'zoom-in' },
      },
    ],
  },
  {
    path: '/timeline/day/:date',
    name: 'timeline-day',
    component: MainLayout,
    children: [
      {
        path: '',
        component: () => import('@/modules/timeline/views/DayDetailView.vue'),
        meta: { title: '日子', depth: 3 },
      },
    ],
  },
  {
    path: '/search',
    name: 'search',
    component: MainLayout,
    children: [
      {
        path: '',
        component: () => import('@/modules/search/views/SearchView.vue'),
        meta: { title: '搜索' },
      },
    ],
  },
  // ---- 管理中心 ----
  {
    path: '/manage',
    component: ManageLayout,
    redirect: '/manage/diaries',
    children: [
      { path: 'diaries', name: 'manage-diaries', component: () => import('@/modules/diary/views/DiaryListView.vue'), meta: { title: '日记管理' } },
      { path: 'diaries/new', name: 'manage-diary-new', component: () => import('@/modules/diary/views/DiaryEditorView.vue'), meta: { title: '新建日记' } },
      { path: 'diaries/:id/edit', name: 'manage-diary-edit', component: () => import('@/modules/diary/views/DiaryEditorView.vue'), meta: { title: '编辑日记' } },
      { path: 'knowledge', redirect: '/manage/knowledge/notes', meta: { title: '知识库' } },
      { path: 'knowledge/categories', name: 'manage-categories', component: () => import('@/modules/knowledge/views/KnowledgeCategoriesView.vue'), meta: { title: '知识分类' } },
      { path: 'knowledge/notes', name: 'manage-notes', component: () => import('@/modules/knowledge/views/KnowledgeNotesView.vue'), meta: { title: '知识笔记' } },
      { path: 'knowledge/notes/new', name: 'manage-note-new', component: () => import('@/modules/knowledge/views/KnowledgeNoteEditorView.vue'), meta: { title: '新建笔记' } },
      { path: 'knowledge/notes/:id/edit', name: 'manage-note-edit', component: () => import('@/modules/knowledge/views/KnowledgeNoteEditorView.vue'), meta: { title: '编辑笔记' } },
      { path: 'study-records', name: 'manage-study', component: () => import('@/modules/study/views/StudyRecordListView.vue'), meta: { title: '学习记录' } },
      { path: 'stages', name: 'manage-stages', component: () => import('@/modules/life-stage/views/LifeStageListView.vue'), meta: { title: '人生阶段' } },
      { path: 'events', name: 'manage-events', component: () => import('@/modules/event/views/EventListView.vue'), meta: { title: '重要事件' } },
      { path: 'albums', name: 'manage-albums', component: () => import('@/modules/media/views/AlbumView.vue'), meta: { title: '相册' } },
      { path: 'data', name: 'manage-data', component: () => import('@/modules/data-management/views/DataManagementView.vue'), meta: { title: '数据管理' } },
      { path: 'settings', name: 'manage-settings', component: () => import('@/modules/settings/views/SettingsView.vue'), meta: { title: '设置' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/timeline' },
]