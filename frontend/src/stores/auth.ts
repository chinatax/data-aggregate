import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, registerApi, getWechatQrcodeApi, wechatCheckApi, getCurrentUserApi, verifyEmailApi } from '@/api/auth'
import type { LoginResponse, LoginRequest, RegisterRequest } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userId = ref<number>(0)
  const username = ref<string>('')
  const nickname = ref<string>('')
  const avatar = ref<string>('')
  const email = ref<string>('')
  const role = ref<string>('')
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const displayName = computed(() => nickname.value || username.value || '未登录')

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    setAuthState(res)
  }

  async function register(data: RegisterRequest) {
    await registerApi(data)
  }

  async function verifyEmail(emailAddr: string, code: string) {
    await verifyEmailApi({ email: emailAddr, code })
  }

  async function fetchWechatQrcode() {
    return await getWechatQrcodeApi()
  }

  async function checkWechatLogin(state: string): Promise<string | null> {
    const res = await wechatCheckApi(state)
    if (res.success && res.token) {
      return res.token
    }
    return null
  }

  async function initAuth() {
    if (!token.value) {
      initialized.value = true
      return
    }
    try {
      const user = await getCurrentUserApi()
      userId.value = user.userId
      username.value = user.username
      nickname.value = user.nickname
      avatar.value = user.avatar
      email.value = user.email
      role.value = user.role
    } catch {
      clearAuth()
    } finally {
      initialized.value = true
    }
  }

  function setAuthState(res: LoginResponse) {
    token.value = res.token
    localStorage.setItem('token', res.token)
    if (res.refreshToken) {
      refreshToken.value = res.refreshToken
      localStorage.setItem('refreshToken', res.refreshToken)
    }
    userId.value = res.userId
    username.value = res.username
    nickname.value = res.nickname
    avatar.value = res.avatar
    email.value = res.email
    role.value = res.role
  }

  function setTokenFromWechat(jwt: string) {
    token.value = jwt
    localStorage.setItem('token', jwt)
  }

  function clearAuth() {
    token.value = ''
    refreshToken.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    userId.value = 0
    username.value = ''
    nickname.value = ''
    avatar.value = ''
    email.value = ''
    role.value = ''
  }

  async function logout() {
    clearAuth()
  }

  return {
    token, refreshToken, userId, username, nickname, avatar, email, role, initialized,
    isLoggedIn, isAdmin, displayName,
    login, register, verifyEmail, fetchWechatQrcode, checkWechatLogin, initAuth, setTokenFromWechat, setAuthState, logout,
  }
})