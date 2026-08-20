import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 60000,
})

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    // 解包后端 Result<T>: {code, message, data} → data
    return res.data ?? res
  },
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      const skipRedirect = (originalRequest as any)._skipAuthRedirect
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        clearAuthAndRedirect(skipRedirect)
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          pendingRequests.push((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post('/api/auth/refresh', { refreshToken })
        const data = res.data?.data || res.data
        if (data?.token) {
          localStorage.setItem('token', data.token)
          if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken)
          }
          pendingRequests.forEach(cb => cb(data.token))
          pendingRequests = []
          originalRequest.headers.Authorization = `Bearer ${data.token}`
          return request(originalRequest)
        } else {
          clearAuthAndRedirect(skipRedirect)
          return Promise.reject(error)
        }
      } catch {
        clearAuthAndRedirect(skipRedirect)
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response?.status === 401) {
      clearAuthAndRedirect((originalRequest as any)?._skipAuthRedirect)
    }

    const msg = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status !== 401) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

function clearAuthAndRedirect(skipRedirect = false) {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  if (skipRedirect) return
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
}

export default request
