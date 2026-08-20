<template>
  <div class="layout">
    <AppHeader />
    <main class="layout-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <keep-alive :include="['HomePage', 'BlogList']">
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.layout {
  min-height: 100vh;
  background: $bg-page;
}

.layout-content {
  padding: calc(56px + #{$gap-lg}) $gap-lg $gap-lg;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity $ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
