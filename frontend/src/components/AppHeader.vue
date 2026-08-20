<template>
  <header class="app-header">
    <div class="header-left">
      <div class="logo" @click="router.push('/')">
        <SiteLogo :size="28" />
        <span class="logo-text">{{ siteStore.siteName }}</span>
      </div>
      <nav class="header-nav">
        <router-link
          v-for="item in menuList"
          :key="item.path"
          :to="item.path"
          :class="['nav-item', { active: activeMenu === item.path }]"
        >
          {{ item.name }}
        </router-link>
      </nav>
    </div>
    <div class="header-right">
      <div class="icon-btn">
        <el-icon><Bell /></el-icon>
      </div>
      <div v-if="authStore.isAdmin" class="icon-btn" @click="router.push('/admin/settings')" title="站点设置">
        <el-icon><Setting /></el-icon>
      </div>

      <!-- 已登录 -->
      <el-dropdown v-if="authStore.isLoggedIn" trigger="click">
        <div class="user">
          <el-avatar :size="28" :src="authStore.avatar" v-if="authStore.avatar" />
          <el-avatar :size="28" :icon="UserFilled" v-else />
          <span>{{ authStore.displayName }}</span>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>
              <el-icon><User /></el-icon>
              <span>{{ authStore.username }}</span>
            </el-dropdown-item>
            <el-dropdown-item v-if="authStore.isAdmin" divided @click="router.push('/admin/monitor')">
              <el-icon><Monitor /></el-icon>
              <span>性能监控</span>
            </el-dropdown-item>
            <el-dropdown-item divided @click="pwdDialogRef?.open()">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- 未登录 -->
      <router-link v-else to="/login" class="login-link">
        <el-button type="primary" size="small">登录</el-button>
      </router-link>
    </div>

    <ChangePasswordDialog ref="pwdDialogRef" />
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import SiteLogo from '@/components/SiteLogo.vue'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import { Bell, Setting, UserFilled, ArrowDown, User, SwitchButton, Monitor, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useSiteStore } from '@/stores/site'
import { ref } from 'vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const siteStore = useSiteStore()
const pwdDialogRef = ref<InstanceType<typeof ChangePasswordDialog>>()

const activeMenu = computed(() => route.path)

const menuList = computed(() => siteStore.channelList.map(c => ({
  path: c.path,
  name: c.name,
})))

async function handleLogout() {
  await authStore.logout()
  siteStore.clearConfig()
  router.push('/')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $white;
  border-bottom: 1px solid $border;
  padding: 0 $gap-lg;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $gap-xl;

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    user-select: none;

    .logo-text {
      font-size: 16px;
      font-weight: 600;
      color: $text-main;
    }
  }

  .header-nav {
    display: flex;
    align-items: center;
    gap: $gap-xs;

    .nav-item {
      padding: 6px $gap-sm;
      font-size: 14px;
      color: $text-sub;
      border-radius: $radius-sm;
      transition: all $ease;

      &:hover {
        color: $text-main;
        background: $bg-hover;
      }

      &.active {
        color: $text-main;
        font-weight: 500;
        background: $bg-hover;
      }
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: $gap-xs;

  .icon-btn {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-sm;
    cursor: pointer;
    color: $text-sub;
    transition: all $ease;

    &:hover {
      background: $bg-hover;
      color: $text-main;
    }

    .el-icon {
      font-size: 18px;
    }
  }

  .user {
    display: flex;
    align-items: center;
    gap: $gap-xs;
    padding: 4px 12px 4px 4px;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: background $ease;

    &:hover {
      background: $bg-hover;
    }

    span {
      font-size: 13px;
      color: $text-sub;
    }

    .arrow-icon {
      font-size: 12px;
      color: $text-sub;
    }
  }

  .login-link {
    text-decoration: none;
    margin-left: $gap-xs;
  }
}
</style>
