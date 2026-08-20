import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import EditorLayout from '@/layout/EditorLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: Layout,
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/views/home/index.vue'),
          meta: { title: '首页', icon: 'HomeFilled' },
        },
        {
          path: 'docx',
          name: 'Docx',
          component: () => import('@/views/docx/index.vue'),
          meta: { title: '文档编辑', icon: 'Document' },
        },
        {
          path: 'ebook',
          name: 'Ebook',
          component: () => import('@/views/ebook/index.vue'),
          meta: { title: '电子书集', icon: 'Reading' },
        },
        {
          path: 'district',
          name: 'District',
          component: () => import('@/views/district/index.vue'),
          meta: { title: '行政区划', icon: 'MapLocation' },
        },
        {
          path: 'contact',
          name: 'Contact',
          component: () => import('@/views/contact/index.vue'),
          meta: { title: '联系我们', icon: 'Service' },
        },
        {
          path: 'blog',
          name: 'Blog',
          component: () => import('@/views/blog/index.vue'),
          meta: { title: '博客', icon: 'Document' }
        },
        {
          path: 'blog/:id',
          name: 'BlogDetail',
          component: () => import('@/views/blog/detail.vue'),
          meta: { title: '博客详情', hidden: true },
        },
        {
          path: 'admin/categories',
          name: 'AdminCategories',
          component: () => import('@/views/admin/categories/index.vue'),
          meta: { title: '分类管理', requiresAdmin: true },
        },
        {
          path: 'admin/monitor',
          name: 'AdminMonitor',
          component: () => import('@/views/admin/monitor/index.vue'),
          meta: { title: '性能监控', requiresAdmin: true },
        },
        {
          path: 'admin/settings',
          name: 'AdminSettings',
          component: () => import('@/views/admin/settings/index.vue'),
          meta: { title: '站点设置', requiresAdmin: true },
        },
      ],
    },
    {
      path: '/blog/editor',
      component: EditorLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'BlogEditor',
          component: () => import('@/views/blog/editor/index.vue'),
          meta: { title: '写博客', hidden: true },
        },
        {
          path: ':id',
          name: 'BlogEditorEdit',
          component: () => import('@/views/blog/editor/index.vue'),
          meta: { title: '编辑博客', hidden: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginPage.vue'),
      meta: { title: '登录', requiresGuest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterPage.vue'),
      meta: { title: '注册', requiresGuest: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
    {
      path: '/home',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const { useAuthStore } = await import('@/stores/auth')
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    await authStore.initAuth()
  }

  if ((to.meta.requiresAuth || to.meta.requiresAdmin) && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Home' })
    return
  }

  if (to.meta.requiresGuest && authStore.isLoggedIn) {
    next({ name: 'Home' })
    return
  }

  next()
})

export default router
