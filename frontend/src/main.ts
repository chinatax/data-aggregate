import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import { useAuthStore } from '@/stores/auth'
import { useSiteStore } from '@/stores/site'
import ElementPlus from 'element-plus'

import './styles/index.scss'

const app = createApp(App)

// 注册 Element Plus
app.use(ElementPlus)

app.use(pinia)
app.use(router)

// 初始化认证状态（恢复登录态）+ 站点配置
const authStore = useAuthStore()
const siteStore = useSiteStore()
Promise.all([authStore.initAuth(), siteStore.fetchConfig()]).then(() => {
  app.mount('#app')
})
