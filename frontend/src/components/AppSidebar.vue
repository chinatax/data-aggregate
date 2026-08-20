<template>
  <aside :style="{ width: isCollapsed ? '64px' : '220px' }" class="app-sidebar">
    <div class="sidebar-header">
      <span v-if="!isCollapsed" class="sidebar-title">导航</span>
      <el-icon class="collapse-btn" @click="toggleCollapse">
        <Fold v-if="!isCollapsed" />
        <Expand v-else />
      </el-icon>
    </div>

    <nav class="sidebar-nav">
      <router-link
        v-for="item in menuList"
        :key="item.path"
        :to="item.path"
        :class="['nav-item', { active: activeMenu === item.path }]"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span v-if="!isCollapsed" class="nav-text">{{ item.name }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, TrendCharts, Reading, MapLocation, Fold, Expand, Document } from '@element-plus/icons-vue'
import { useSiteStore } from '@/stores/site'

const route = useRoute()
const siteStore = useSiteStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const iconMap: Record<string, any> = { HomeFilled, TrendCharts, Reading, MapLocation, Document }

const menuList = computed(() => siteStore.channelList.map(c => ({
  path: c.path,
  name: c.name,
  icon: iconMap[c.icon] || Document,
})))

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-sidebar {
  background: $bg-card;
  border-right: 1px solid $border-light;
  transition: width $ease;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $gap-md;
  border-bottom: 1px solid $border-light;
  flex-shrink: 0;

  .sidebar-title {
    font-size: 12px;
    font-weight: 600;
    color: $text-muted;
    letter-spacing: 1px;
  }

  .collapse-btn {
    font-size: 15px;
    color: $text-muted;
    cursor: pointer;
    padding: 6px;
    border-radius: $radius;
    transition: all $ease;

    &:hover {
      color: $blue;
      background: $bg-hover;
    }
  }
}

.sidebar-nav {
  padding: $gap-sm;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: $gap-sm;
  padding: 10px $gap-md;
  border-radius: $radius;
  color: $text-sub;
  text-decoration: none;
  transition: all $ease;
  cursor: pointer;
  position: relative;

  .el-icon {
    font-size: 18px;
    flex-shrink: 0;
  }

  .nav-text {
    font-size: 14px;
    white-space: nowrap;
  }

  &:hover {
    background: $bg-hover;
    color: $text-main;
  }

  &.active {
    background: $bg-active;
    color: $blue;
    font-weight: 500;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 8px;
      bottom: 8px;
      width: 3px;
      background: $blue;
      border-radius: 0 2px 2px 0;
    }
  }
}
</style>
