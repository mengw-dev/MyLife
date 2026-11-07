<script setup lang="ts">
/**
 * 数据管理页：普通导出 / AI 分析资料包 / 完整备份与恢复。
 * 所有操作仅作用于当前登录用户的数据（后端按当前会话过滤）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus'
import { dataApi } from '../api/data.api'
import type { ExportFormat } from '../api/data.api'

const exporting = ref(false)
const aiLoading = ref(false)
const backupLoading = ref(false)
const restoring = ref(false)

const exportForm = reactive<{
  format: ExportFormat
  include: string[]
}>({
  format: 'markdown',
  include: ['diary', 'note', 'study', 'event', 'stage'],
})
const from = ref('')
const to = ref('')

const restoreFiles = ref<UploadUserFile[]>([])

const formatOptions: Array<{ label: string; value: ExportFormat }> = [
  { label: 'Markdown', value: 'markdown' },
  { label: 'JSON', value: 'json' },
  { label: 'ZIP', value: 'zip' },
]

const includeOptions: Array<{ label: string; value: string }> = [
  { label: '日记', value: 'diary' },
  { label: '知识笔记', value: 'note' },
  { label: '学习记录', value: 'study' },
  { label: '重要事件', value: 'event' },
  { label: '人生阶段', value: 'stage' },
]

async function doExport() {
  if (exportForm.include.length === 0) {
    ElMessage.warning('请至少选择一项导出内容')
    return
  }
  exporting.value = true
  try {
    const result = await dataApi.exportFile({
      format: exportForm.format,
      include: [...exportForm.include],
      ...(from.value ? { from: from.value } : {}),
      ...(to.value ? { to: to.value } : {}),
    })
    ElMessage.success(`已导出：${result.fileName}`)
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    exporting.value = false
  }
}

async function doAiPackage() {
  aiLoading.value = true
  try {
    const result = await dataApi.buildAiPackage()
    ElMessage.success(`已下载：${result.fileName}`)
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    aiLoading.value = false
  }
}

async function doBackup() {
  backupLoading.value = true
  try {
    const result = await dataApi.exportBackup()
    ElMessage.success(`备份完成：${result.fileName}`)
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    backupLoading.value = false
  }
}

async function onRestoreChange(upload: UploadFile) {
  const file = upload.raw
  if (!file) return
  restoring.value = true
  try {
    const result = await dataApi.validateBackup(file)
    if (!result.valid) {
      ElMessage.warning(result.message)
      restoreFiles.value = []
      return
    }
    try {
      await ElMessageBox.confirm(
        `即将用「${file.name}」覆盖当前数据，此操作不可撤销。确定继续吗？`,
        '恢复备份',
        { type: 'warning', confirmButtonText: '继续恢复', cancelButtonText: '取消' },
      )
    } catch {
      restoreFiles.value = []
      return
    }
    await dataApi.restoreBackup(file)
    ElMessage.success('数据恢复完成')
    restoreFiles.value = []
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    restoring.value = false
  }
}
</script>

<template>
  <div class="app-page data-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">数据管理</h1>
        <p class="page-sub">导出、备份与恢复你的数据，所有操作仅影响当前用户</p>
      </div>
    </header>

    <!-- 普通导出 -->
    <section class="soft-card data-card">
      <h2 class="card-title">普通导出</h2>
      <div class="form-block">
        <span class="field-label">导出格式</span>
        <ElRadioGroup v-model="exportForm.format">
          <ElRadio v-for="f in formatOptions" :key="f.value" :value="f.value">{{ f.label }}</ElRadio>
        </ElRadioGroup>
      </div>
      <div class="form-block">
        <span class="field-label">包含内容</span>
        <ElCheckboxGroup v-model="exportForm.include">
          <ElCheckbox v-for="o in includeOptions" :key="o.value" :value="o.value">{{ o.label }}</ElCheckbox>
        </ElCheckboxGroup>
      </div>
      <div class="form-block">
        <span class="field-label">日期范围（可选）</span>
        <div class="range-row">
          <ElDatePicker v-model="from" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
          <span class="range-sep">至</span>
          <ElDatePicker v-model="to" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        </div>
      </div>
      <div class="card-actions">
        <ElButton type="primary" :loading="exporting" @click="doExport">导出</ElButton>
        <span class="card-hint">尚未对接真实后端时以 Mock 验证下载流程</span>
      </div>
    </section>

    <!-- AI 分析资料包 -->
    <section class="soft-card data-card">
      <h2 class="card-title">AI 分析资料包</h2>
      <p class="card-desc">将你的日记、笔记与学习记录打包，供 AI 工具进行深度分析（不含账号口令）。</p>
      <div class="card-actions">
        <ElButton type="primary" :loading="aiLoading" @click="doAiPackage">下载资料包</ElButton>
      </div>
    </section>

    <!-- 完整备份 -->
    <section class="soft-card data-card">
      <h2 class="card-title">完整备份</h2>
      <p class="card-desc">导出包含全部数据的备份文件，或将之前导出的备份恢复到当前账号。</p>
      <div class="card-actions">
        <ElButton :loading="backupLoading" @click="doBackup">导出备份文件</ElButton>
        <div class="restore-box">
          <ElUpload
            v-model:file-list="restoreFiles"
            :auto-upload="false"
            :limit="1"
            accept=".zip,.json"
            :on-change="onRestoreChange"
          >
            <ElButton :loading="restoring">选择文件恢复</ElButton>
          </ElUpload>
          <span class="card-hint">支持 .zip / .json 备份文件</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.data-page {
  max-width: 860px;
}
.data-card {
  margin-bottom: 16px;
}
.card-title {
  font-size: 16px;
  margin: 0 0 12px;
}
.card-desc {
  margin: 0 0 12px;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.7;
}
.form-block {
  margin-bottom: 14px;
}
.range-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.range-sep {
  color: var(--muted);
  font-size: 13px;
}
.card-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.card-hint {
  color: var(--muted);
  font-size: 12.5px;
}
.restore-box {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>