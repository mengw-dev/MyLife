<script setup lang="ts">
import AppHeader from '@/shared/components/AppHeader.vue'
</script>

<template>
  <div class="main-layout">
    <AppHeader />
    <main class="main-body">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: radial-gradient(1200px 500px at 50% -8%, rgba(125, 107, 79, 0.045), transparent 60%), var(--bg);
}
.main-body {
  min-height: calc(100vh - 56px);
}
</style>