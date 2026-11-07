<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { PATHS } from '@/shared/constants/paths'

const auth = useAuthStore()
const router = useRouter()

const form = reactive({ username: '', password: '', confirm: '', nickname: '' })
const formRef = ref<FormInstance>()
const submitting = ref(false)

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4-20 位字母、数字或下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度 8-64 位', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v === form.password ? cb() : cb(new Error('两次输入的密码不一致'))),
      trigger: 'blur',
    },
  ],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await auth.register(form.username, form.password, form.nickname || undefined)
    ElMessage.success('注册成功，欢迎加入 MyLife')
    router.push(PATHS.timeline)
  } catch {
    // 统一拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-brand">MyLife</h1>
      <p class="auth-slogan">从今天开始，认真记录自己的一生</p>

      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <ElFormItem label="用户名" prop="username">
          <ElInput v-model="form.username" placeholder="4-20 位字母、数字或下划线" autocomplete="username" />
        </ElFormItem>
        <ElFormItem label="昵称（可选）" prop="nickname">
          <ElInput v-model="form.nickname" placeholder="别人怎么称呼你" />
        </ElFormItem>
        <ElFormItem label="密码" prop="password">
          <ElInput v-model="form.password" type="password" show-password placeholder="至少 8 位" autocomplete="new-password" />
        </ElFormItem>
        <ElFormItem label="确认密码" prop="confirm">
          <ElInput v-model="form.confirm" type="password" show-password placeholder="再输入一次" autocomplete="new-password" @keyup.enter="submit" />
        </ElFormItem>
        <ElButton class="submit-btn" type="primary" :loading="submitting" @click="submit">注册</ElButton>
      </ElForm>

      <p class="auth-switch">
        已有账号？
        <RouterLink :to="PATHS.login">直接登录</RouterLink>
      </p>
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
</style>