import { createPinia } from 'pinia'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const pageTitle = ref('数据聚合智能应用')

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setPageTitle = (title: string) => {
    pageTitle.value = title
  }

  return {
    sidebarCollapsed,
    pageTitle,
    toggleSidebar,
    setPageTitle,
  }
})

export default createPinia()
