<script setup lang="ts">
/**
 * 登录页：进入 MyLife 的门。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { PATHS } from '@/shared/constants/paths'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

/** 是否 Mock 演示模式（模板中不能直接写 import.meta） */
const isMock = import.meta.env.VITE_USE_MOCK === 'true'

const form = reactive({ username: '', password: '' })
const formRef = ref<FormInstance>()
const submitting = ref(false)

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success(`欢迎回来，${auth.displayName}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : PATHS.timeline
    router.push(redirect)
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-brand">MyLife</h1>
      <p class="auth-slogan">记录生活，积累知识，回顾成长</p>

      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <ElFormItem label="用户名" prop="username">
          <ElInput v-model="form.username" placeholder="用户名" autocomplete="username" />
        </ElFormItem>
        <ElFormItem label="密码" prop="password">
          <ElInput v-model="form.password" type="password" show-password placeholder="密码" autocomplete="current-password" @keyup.enter="submit" />
        </ElFormItem>
        <ElButton class="submit-btn" type="primary" :loading="submitting" @click="submit">登录</ElButton>
      </ElForm>

      <p class="auth-switch">
        还没有账号？
        <RouterLink :to="PATHS.register">注册一个</RouterLink>
      </p>
      <p v-if="isMock" class="mock-hint">演示账号：demo / 12345678</p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(1000px 480px at 50% -10%, rgba(125, 107, 79, 0.06), transparent 60%), var(--bg);
}
.auth-card {
  width: min(380px, calc(100vw - 48px));
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 36px 32px 28px;
}
.auth-brand {
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 500;
  text-align: center;
}
.auth-slogan {
  text-align: center;
  color: var(--muted);
  font-size: 13px;
  margin: 6px 0 24px;
}
.submit-btn {
  width: 100%;
  margin-top: 4px;
}
.auth-switch {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}
.mock-hint {
  margin-top: 10px;
  text-align: center;
  font-size: 12px;
  color: var(--accent-blue);
}
</style>