<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-hero">
        <div class="hero-logo" @click="$router.push('/')">{{ siteStore.siteName }}</div>
        <p class="hero-desc" :style="{ color: siteStore.registerSloganColor }">{{ siteStore.registerSlogan }}</p>
      </div>

      <div class="register-card">
        <h2 class="card-title">注册账号</h2>

        <div class="form-area" autocomplete="off">
          <div class="form-item">
            <el-input v-model="form.username" placeholder="用户名（3-50位字母数字下划线）" size="large" clearable
              maxlength="50" show-word-limit autocomplete="off" />
          </div>
          <div class="form-item">
            <el-input v-model="form.email" placeholder="邮箱" size="large" clearable autocomplete="off" />
          </div>
          <div class="form-item">
            <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" size="large"
              show-password autocomplete="new-password" />
          </div>
          <div class="form-item">
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large"
              show-password autocomplete="new-password" />
          </div>

          <!-- 已经发送验证码则显示验证码输入 -->
          <div v-if="codeSent" class="form-item verify-row">
            <el-input v-model="form.verifyCode" placeholder="请输入邮箱验证码" size="large" maxlength="6"
              style="flex: 1" />
            <span class="countdown" v-if="countdown > 0">{{ countdown }}秒后重发</span>
            <el-button v-else size="large" :disabled="countdown > 0" @click="sendCode">
              重新发送
            </el-button>
          </div>

          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">
            {{ codeSent ? '完成注册' : '获取验证码' }}
          </el-button>
        </div>

        <div class="form-footer">
          <span>已有账号？<router-link to="/login" class="link">立即登录</router-link></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useSiteStore } from '@/stores/site'

const router = useRouter()
const authStore = useAuthStore()
const siteStore = useSiteStore()

const form = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  verifyCode: '',
})

const loading = ref(false)
const codeSent = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

async function sendCode() {
  if (!validateForm()) return
  loading.value = true
  try {
    await authStore.register({
      username: form.value.username.trim(),
      email: form.value.email.trim(),
      password: form.value.password,
    })
    codeSent.value = true
    startCountdown()
    ElMessage.success('验证码已发送至邮箱（开发模式请查看后端控制台日志）')
  } catch {
    // 错误已在 request 拦截器中提示
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (codeSent.value) {
    // 验证验证码
    if (!form.value.verifyCode.trim()) {
      ElMessage.warning('请输入验证码')
      return
    }
    loading.value = true
    try {
      await authStore.verifyEmail(form.value.email.trim(), form.value.verifyCode.trim())
      // 自动登录
      await authStore.login({
        username: form.value.username.trim(),
        password: form.value.password,
      })
      ElMessage.success('注册成功')
      router.push('/')
    } catch {
      // 错误已在拦截器中提示
    } finally {
      loading.value = false
    }
  } else {
    await sendCode()
  }
}

function validateForm(): boolean {
  const username = form.value.username.trim()
  if (!username || username.length < 3 || username.length > 50) {
    ElMessage.warning('用户名需3-50位')
    return false
  }
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    ElMessage.warning('用户名只允许字母、数字、下划线')
    return false
  }
  if (!form.value.email || !/^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(form.value.email)) {
    ElMessage.warning('邮箱格式不正确')
    return false
  }
  if (!form.value.password || form.value.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return false
  }
  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return false
  }
  return true
}

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.register-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.register-container {
  display: flex;
  align-items: stretch;
  background: $white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  width: 720px;
  min-height: 480px;
}

.register-hero {
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

.register-card {
  flex: 0 0 400px;
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-main;
  margin-bottom: 24px;
  text-align: center;
}

.form-area {
  .form-item {
    margin-bottom: 16px;
  }

  .verify-row {
    display: flex;
    gap: 8px;
    align-items: center;

    .countdown {
      font-size: 13px;
      color: $gray-400;
      white-space: nowrap;
    }
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
</style>
