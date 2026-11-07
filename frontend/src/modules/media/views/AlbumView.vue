<script setup lang="ts">
/**
 * 相册页面：照片网格 + 详情弹窗 + 上传 + 删除（引用保护 409 由拦截器统一提示）。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { mediaApi } from '../api/media.api'
import type { MediaFile } from '@/shared/types/entities'
import AppLoading from '@/shared/components/AppLoading.vue'
import AppEmpty from '@/shared/components/AppEmpty.vue'
import AppPagination from '@/shared/components/AppPagination.vue'
import AppDialog from '@/shared/components/AppDialog.vue'
import MediaCard from '../components/MediaCard.vue'

const records = ref<MediaFile[]>([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)
const error = ref(false)

const uploadOpen = ref(false)
const uploadFile = ref<File | null>(null)
const uploadPreview = ref('')
const uploadDescription = ref('')
const uploading = ref(false)

const detailOpen = ref(false)
const detail = ref<MediaFile | null>(null)

function fmtSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

function fmtDate(iso: string): string {
  return iso.slice(0, 16).replace('T', ' ')
}

async function load() {
  loading.value = true
  error.value = false
  try {
    const res = await mediaApi.list(page.value, pageSize.value)
    records.value = res.records
    total.value = res.total
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function onPageChange(payload: { page: number; pageSize: number }) {
  page.value = payload.page
  pageSize.value = payload.pageSize
  load()
}

function openUpload() {
  uploadFile.value = null
  uploadPreview.value = ''
  uploadDescription.value = ''
  uploadOpen.value = true
}

function cleanupPreview() {
  if (uploadPreview.value) {
    URL.revokeObjectURL(uploadPreview.value)
    uploadPreview.value = ''
  }
  uploadFile.value = null
}

function closeUpload() {
  cleanupPreview()
  uploadOpen.value = false
}

function onUploadChange(upload: UploadFile) {
  const file = upload.raw
  if (!file) return
  cleanupPreview()
  uploadFile.value = file
  uploadPreview.value = URL.createObjectURL(file)
}

async function submitUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择一张图片')
    return
  }
  uploading.value = true
  try {
    await mediaApi.upload(uploadFile.value, uploadDescription.value || undefined)
    ElMessage.success('上传成功')
    cleanupPreview()
    uploadOpen.value = false
    page.value = 1
    load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    uploading.value = false
  }
}

function openDetail(media: MediaFile) {
  detail.value = media
  detailOpen.value = true
}

function closeDetail() {
  detailOpen.value = false
  detail.value = null
}

async function confirmDelete(media: MediaFile) {
  try {
    await ElMessageBox.confirm(`确定删除「${media.originalName}」吗？删除后不可恢复。`, '删除照片', { type: 'warning' })
  } catch {
    return
  }
  try {
    await mediaApi.remove(media.id)
    ElMessage.success('已删除')
    if (detail.value?.id === media.id) closeDetail()
    if (records.value.length === 1 && page.value > 1) page.value -= 1
    load()
  } catch {
    // 删除失败（如照片仍被引用 409）由拦截器统一提示
  }
}

onMounted(load)
</script>

<template>
  <div class="app-page album-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">相册</h1>
        <p class="page-sub">集中查看与整理你上传的照片</p>
      </div>
      <ElButton type="primary" @click="openUpload">上传照片</ElButton>
    </header>

    <AppLoading v-if="loading" text="正在加载照片…" />
    <div v-else-if="error" class="album-message">
      <AppEmpty text="照片加载失败，请稍后重试" />
      <ElButton @click="load">重新加载</ElButton>
    </div>
    <AppEmpty v-else-if="records.length === 0" text="还没有照片，点击右上角上传第一张吧" />
    <template v-else>
      <div class="media-grid-fallback">
        <MediaCard v-for="m in records" :key="m.id" :media="m" @select="openDetail" />
      </div>
      <AppPagination :page="page" :page-size="pageSize" :total="total" @change="onPageChange" />
    </template>

    <!-- 详情弹窗 -->
    <AppDialog :visible="detailOpen" title="照片详情" @close="closeDetail">
      <div v-if="detail" class="detail-body">
        <img class="detail-img" :src="mediaApi.url(detail.id)" :alt="detail.originalName" />
        <dl class="detail-meta">
          <div class="detail-row"><dt>文件名</dt><dd>{{ detail.originalName }}</dd></div>
          <div class="detail-row"><dt>类型</dt><dd>{{ detail.mimeType }}</dd></div>
          <div class="detail-row"><dt>大小</dt><dd>{{ fmtSize(detail.sizeBytes) }}</dd></div>
          <div class="detail-row"><dt>上传时间</dt><dd>{{ fmtDate(detail.createdAt) }}</dd></div>
          <div class="detail-row"><dt>描述</dt><dd>{{ detail.description || '—' }}</dd></div>
        </dl>
      </div>
      <template #footer>
        <ElButton @click="closeDetail">关闭</ElButton>
        <ElButton v-if="detail" type="danger" plain @click="confirmDelete(detail)">删除</ElButton>
      </template>
    </AppDialog>

    <!-- 上传弹窗 -->
    <AppDialog :visible="uploadOpen" title="上传照片" @close="closeUpload">
      <div class="upload-body">
        <ElUpload
          :auto-upload="false"
          :show-file-list="false"
          :limit="1"
          accept="image/*"
          :on-change="onUploadChange"
        >
          <ElButton>选择图片</ElButton>
        </ElUpload>
        <div v-if="uploadPreview" class="upload-preview">
          <img :src="uploadPreview" :alt="uploadFile?.name ?? ''" />
        </div>
        <ElInput
          v-model="uploadDescription"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          placeholder="描述（可选）"
        />
      </div>
      <template #footer>
        <ElButton @click="closeUpload">取消</ElButton>
        <ElButton type="primary" :loading="uploading" @click="submitUpload">确认上传</ElButton>
      </template>
    </AppDialog>
  </div>
</template>

<style scoped>
.album-page {
  max-width: 1080px;
}
.album-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.detail-img {
  width: 100%;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  background: var(--bg-soft);
}
.detail-meta {
  margin: 0;
}
.detail-row {
  display: flex;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}
.detail-row:last-child {
  border-bottom: none;
}
.detail-row dt {
  flex: none;
  width: 72px;
  color: var(--muted);
}
.detail-row dd {
  margin: 0;
  color: var(--ink-2);
  word-break: break-all;
}
.upload-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.upload-preview img {
  width: 100%;
  max-height: 260px;
  object-fit: contain;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  background: var(--bg-soft);
}
</style>