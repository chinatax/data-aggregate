<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-hero">
        <div class="hero-logo" @click="$router.push('/')">{{ siteStore.siteName }}</div>
        <p class="hero-desc" :style="{ color: siteStore.loginSloganColor }">{{ siteStore.loginSlogan }}</p>
      </div>

      <div class="login-card">
        <div class="card-tabs">
          <div :class="['tab', { active: activeTab === 'password' }]" @click="activeTab = 'password'">
            账号登录
          </div>
          <div :class="['tab', { active: activeTab === 'wechat' }]" @click="activeTab = 'wechat'">
            微信登录
          </div>
        </div>

        <!-- 密码登录 -->
        <template v-if="activeTab === 'password'">
          <div class="form-area" autocomplete="off">
            <div class="form-item">
              <el-input v-model="loginForm.username" placeholder="用户名" size="large" clearable autocomplete="off" />
            </div>
            <div class="form-item">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large"
                show-password autocomplete="new-password" @keyup.enter="handleLogin" />
            </div>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </div>
          <div class="form-footer">
            <span>还没有账号？<router-link to="/register" class="link">立即注册</router-link></span>
          </div>
        </template>

        <!-- 微信登录 -->
        <template v-if="activeTab === 'wechat'">
          <div class="wechat-area">
            <div class="qr-container" v-loading="qrLoading">
              <img v-if="qrDataUrl" :src="qrDataUrl" class="qr-img" alt="微信扫码登录" />
              <p v-if="qrError" class="qr-error">{{ qrError }}</p>
            </div>
            <p class="wechat-hint">请使用微信扫描二维码</p>
            <p class="wechat-follow-hint">如提示需关注公众号，请先关注测试号后再扫码</p>
            <p class="wechat-status" v-if="pollingState">
              <el-icon class="loading-icon"><Loading /></el-icon>
              等待扫码确认...
            </p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useSiteStore } from '@/stores/site'
import QRCode from 'qrcode'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const siteStore = useSiteStore()

const activeTab = ref<'password' | 'wechat'>('password')
const loading = ref(false)

// 密码登录
const loginForm = ref({ username: '', password: '' })

async function handleLogin() {
  if (!loginForm.value.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    await authStore.login({
      username: loginForm.value.username.trim(),
      password: loginForm.value.password,
    })
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch {
    // 错误已在 request 拦截器中提示
  } finally {
    loading.value = false
  }
}

// 微信登录
const qrLoading = ref(false)
const qrError = ref('')
const qrDataUrl = ref('')
const pollingState = ref(false)
let pollingTimer: ReturnType<typeof setInterval> | null = null
let currentState = ''

async function loadWechatQrcode() {
  qrLoading.value = true
  qrError.value = ''
  qrDataUrl.value = ''
  stopPolling()
  try {
    const res = await authStore.fetchWechatQrcode()
    currentState = res.state
    const dataUrl = await QRCode.toDataURL(res.authUrl, {
      width: 240,
      margin: 1,
      errorCorrectionLevel: 'L',
    })
    qrDataUrl.value = dataUrl
    qrLoading.value = false
    startPolling()
  } catch {
    qrLoading.value = false
    qrError.value = '二维码加载失败，请重试'
  }
}

function startPolling() {
  pollingState.value = true
  pollingTimer = setInterval(async () => {
    try {
      const token = await authStore.checkWechatLogin(currentState)
      if (token) {
        stopPolling()
        authStore.setTokenFromWechat(token)
        await authStore.initAuth()
        const redirect = (route.query.redirect as string) || '/'
        router.push(redirect)
      }
    } catch {
      // 继续轮询
    }
  }, 2000)
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
  pollingState.value = false
}

watch(activeTab, (tab) => {
  if (tab === 'wechat') {
    loadWechatQrcode()
  } else {
    stopPolling()
  }
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.login-container {
  display: flex;
  align-items: stretch;
  background: $white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  width: 720px;
  min-height: 440px;
}

.login-hero {
  flex: 1;
  background: linear-gradient(135deg, #1677FF 0%, #38BDF8 100%);
  color: $white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;

  .hero-logo {
    font-size: 22px;
    font-weight: 700;
    margin-bottom: 12px;
    letter-spacing: 2px;
    cursor: pointer;
  }

  .hero-desc {
    font-size: 14px;
    opacity: 0.6;
  }
}

.login-card {
  flex: 0 0 400px;
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
}

.card-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 32px;
  border-bottom: 2px solid $border;

  .tab {
    flex: 1;
    text-align: center;
    padding: 10px 0;
    font-size: 15px;
    color: $text-sub;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -2px;
    transition: all 0.2s;

    &:hover {
      color: $text-main;
    }

    &.active {
      color: $text-main;
      font-weight: 600;
      border-bottom-color: #38BDF8;
    }
  }
}

.form-area {
  .form-item {
    margin-bottom: 16px;
  }

  .submit-btn {
    width: 100%;
    margin-top: 8px;
    height: 44px;
    font-size: 16px;
    border-radius: 8px;
  }
}

.form-footer {
  text-align: center;
  margin-top: auto;
  padding-top: 16px;
  font-size: 14px;
  color: $text-sub;

  .link {
    color: #38BDF8;
    text-decoration: none;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}

.wechat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;

  .qr-container {
    width: 200px;
    height: 200px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid $border;
    border-radius: 8px;
    padding: 8px;
    background: #fff;
  }

  .qr-img {
    display: block;
    width: 200px;
    height: 200px;
  }

  .qr-error {
    color: #f56c6c;
    font-size: 14px;
    text-align: center;
  }

  .wechat-hint {
    font-size: 14px;
    color: $text-sub;
  }

  .wechat-follow-hint {
    font-size: 12px;
    color: #e6a23c;
    background: #fdf6ec;
    padding: 6px 12px;
    border-radius: 4px;
  }

  .wechat-status {
    font-size: 13px;
    color: #38BDF8;
    display: flex;
    align-items: center;
    gap: 6px;

    .loading-icon {
      animation: spin 1s linear infinite;
    }
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
