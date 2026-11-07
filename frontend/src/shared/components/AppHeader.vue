<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { PATHS } from '@/shared/constants/paths'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const initials = computed(() => auth.displayName.slice(0, 1).toUpperCase())

function go(path: string) {
  router.push(path)
}

async function onCommand(cmd: string) {
  if (cmd === 'settings') return go(PATHS.settings)
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '退出登录', { type: 'warning' })
    await auth.logout()
    router.push(PATHS.login)
  }
}

const active = computed(() => {
  if (route.path.startsWith('/manage')) return 'manage'
  if (route.path.startsWith('/search')) return 'search'
  return 'timeline'
})
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <button class="brand" type="button" aria-label="回到人生时间线" @click="go(PATHS.timeline)">
        <span class="brand-mark">MyLife</span>
      </button>

      <nav class="header-nav" aria-label="主导航">
        <button
          type="button"
          class="nav-item"
          :class="{ active: active === 'timeline' }"
          @click="go(PATHS.timeline)"
        >人生时间线</button>
        <button
          type="button"
          class="nav-item"
          :class="{ active: active === 'search' }"
          @click="go(PATHS.search)"
        >搜索</button>
        <button
          type="button"
          class="nav-item"
          :class="{ active: active === 'manage' }"
          @click="go(PATHS.manage)"
        >管理我的记录</button>
      </nav>

      <ElDropdown trigger="click" @command="onCommand">
        <button type="button" class="avatar-btn" aria-label="用户菜单">
          <ElAvatar :size="28" class="avatar">{{ initials }}</ElAvatar>
          <span class="avatar-name">{{ auth.displayName }}</span>
        </button>
        <template #dropdown>
          <ElDropdownMenu>
            <ElDropdownItem command="settings">个人设置</ElDropdownItem>
            <ElDropdownItem command="logout" divided>退出登录</ElDropdownItem>
          </ElDropdownMenu>
        </template>
      </ElDropdown>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 40;
  background: rgba(250, 248, 243, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}
.header-inner {
  max-width: 1080px;
  margin: 0 auto;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 24px;
}
.brand {
  background: none;
  border: none;
  padding: 0;
}
.brand-mark {
  font-family: var(--font-serif);
  font-size: 19px;
  letter-spacing: 0.04em;
  color: var(--ink);
}
.header-nav {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}
.nav-item {
  background: none;
  border: none;
  color: var(--ink-2);
  font-size: 13.5px;
  padding: 6px 12px;
  border-radius: 999px;
  transition: background var(--dur) ease, color var(--dur) ease;
}
.nav-item:hover {
  color: var(--accent-deep);
  background: var(--accent-soft);
}
.nav-item.active {
  color: var(--accent-deep);
  background: var(--accent-soft);
  font-weight: 600;
}
.avatar-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: var(--ink-2);
  font-size: 13px;
}
.avatar {
  background: var(--accent);
  font-size: 12px;
}
.avatar-name {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>