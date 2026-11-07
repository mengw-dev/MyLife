<script setup lang="ts">
/**
 * 个人设置：账号信息 + 修改密码 + 注销账号。
 */
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/modules/auth/api/auth.api'
import { PATHS } from '@/shared/constants/paths'
import { toDateKey } from '@/shared/utils/date'

const auth = useAuthStore()
const router = useRouter()

const createdAt = computed(() => (auth.user ? toDateKey(new Date(auth.user.createdAt)) : ''))

const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changing = ref(false)

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码长度至少 6 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }],
}

/** 清除本地登录态（成功后统一跳登录，忽略二次登出错误） */
async function clearSession() {
  try {
    await auth.logout()
  } catch {
    // 会话可能已失效，忽略
  }
}

async function changePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  changing.value = true
  try {
    await authApi.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    await clearSession()
    router.replace(PATHS.login)
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    changing.value = false
  }
}

async function deleteAccount() {
  try {
    await ElMessageBox.confirm('确定要注销账号吗？注销后所有数据将被删除且无法恢复。', '注销账号', {
      type: 'warning',
      confirmButtonText: '继续',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await ElMessageBox.confirm('再次确认：真的要注销账号吗？此操作无法撤销。', '最后确认', {
      type: 'error',
      confirmButtonText: '确认注销',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await authApi.deleteAccount()
    ElMessage.success('账号已注销')
    await clearSession()
    router.replace(PATHS.login)
  } catch {
    // 错误提示由拦截器统一处理
  }
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">设置</h1>

    <div class="card">
      <h2 class="card-title">账号信息</h2>
      <dl class="info-list">
        <div class="info-row">
          <dt>用户名</dt>
          <dd>{{ auth.user?.username ?? '—' }}</dd>
        </div>
        <div class="info-row">
          <dt>昵称</dt>
          <dd>{{ auth.user?.nickname ?? '—' }}</dd>
        </div>
        <div class="info-row">
          <dt>注册时间</dt>
          <dd>{{ createdAt || '—' }}</dd>
        </div>
      </dl>
      <p class="info-note">昵称暂不支持修改（后端暂未提供改昵称接口）。</p>
    </div>

    <div class="card">
      <h2 class="card-title">修改密码</h2>
      <ElForm ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="pwd-form">
        <ElFormItem label="原密码" prop="oldPassword">
          <ElInput v-model="pwdForm.oldPassword" type="password" show-password placeholder="原密码" autocomplete="current-password" />
        </ElFormItem>
        <ElFormItem label="新密码" prop="newPassword">
          <ElInput v-model="pwdForm.newPassword" type="password" show-password placeholder="新密码（至少 6 位）" autocomplete="new-password" />
        </ElFormItem>
        <ElFormItem label="确认新密码" prop="confirmPassword">
          <ElInput v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" autocomplete="new-password" />
        </ElFormItem>
        <ElButton type="primary" :loading="changing" @click="changePassword">修改密码</ElButton>
      </ElForm>
    </div>

    <div class="card danger-card">
      <h2 class="card-title danger-title">危险操作</h2>
      <p class="danger-desc">注销账号将永久删除所有数据，且无法恢复。请谨慎操作。</p>
      <ElButton type="danger" @click="deleteAccount">注销账号</ElButton>
    </div>
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 560px;
}
.page-title {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 500;
  color: var(--ink);
}
.card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin: 0;
}
.info-list {
  margin: 0;
}
.info-row {
  display: flex;
  gap: 18px;
  padding: 7px 0;
  border-bottom: 1px solid var(--line);
}
.info-row:last-child {
  border-bottom: none;
}
.info-row dt {
  width: 72px;
  color: var(--muted);
  font-size: 13px;
}
.info-row dd {
  margin: 0;
  color: var(--ink);
  font-size: 13.5px;
}
.info-note {
  margin: 0;
  font-size: 12.5px;
  color: var(--muted);
}
.pwd-form {
  max-width: 360px;
}
.danger-card {
  border-color: var(--line-strong);
}
.danger-title {
  color: var(--danger);
}
.danger-desc {
  margin: 0;
  font-size: 13px;
  color: var(--muted);
}
</style>