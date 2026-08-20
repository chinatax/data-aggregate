<template>
  <div class="page-container">
    <h1 class="hero-title">电子书集</h1>

    <div class="ebook-layout">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
        </div>

        <div v-show="!sidebarCollapsed" class="sidebar-content">
          <Transition name="slide-up">
            <div v-if="!showFavorites" class="sidebar-upper">
              <el-input
                v-model="keyword"
                placeholder="搜索书名或作者..."
                :prefix-icon="Search"
                clearable
                size="default"
                @keyup.enter="doSearch"
                @clear="doSearch"
              />

              <ul class="category-list">
                <li
                  :class="{ active: activeCategory === null }"
                  @click="selectCategory(null)"
                >
                  <span class="cat-name">全部</span>
                  <span class="cat-count">{{ totalBooks }}</span>
                </li>
                <li
                  v-for="cat in categories"
                  :key="cat.id"
                  :class="{ active: activeCategory === cat.id }"
                  @click="selectCategory(cat.id)"
                >
                  <span class="cat-name">{{ cat.name }}</span>
                  <span class="cat-count">{{ cat.count }}</span>
                </li>
              </ul>
            </div>
          </Transition>

          <div
            class="fav-filter"
            :class="{ active: showFavorites }"
            @click="toggleFavorites"
          >
            <el-icon><Star /></el-icon>
            <span>我的收藏{{ showFavorites ? ' (点击返回)' : '' }}</span>
          </div>
        </div>
      </aside>

      <main class="main-content">
        <div class="result-bar">
          <span class="result-count" v-if="showFavorites">我的收藏 ({{ books.length }})</span>
          <span class="result-count" v-else>共 {{ totalCount }} 本电子书</span>
        </div>

        <div class="book-grid" v-if="books.length > 0">
          <div
            v-for="book in books"
            :key="book.id"
            class="book-card"
          >
            <div class="card-body">
              <h3 class="book-title" :title="book.title">{{ book.title }}</h3>
              <p class="book-author" v-if="book.author">{{ book.author }}</p>
            </div>
            <div class="card-actions">
              <el-button
                :type="favoritedIds.includes(book.id) ? 'danger' : 'default'"
                size="small"
                circle
                @click="handleFavorite(book)"
              >
                <el-icon><Star /></el-icon>
              </el-button>
              <el-button
                type="primary"
                size="small"
                plain
                @click="handleDownload(book)"
              >
                <el-icon class="mr-4"><Download /></el-icon>下载
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-if="!initialLoading && books.length === 0" description="未找到相关电子书" />

        <div ref="sentinel" class="scroll-sentinel" v-if="hasMore">
          <span v-if="loadingMore">加载中...</span>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Fold, Expand, Star, Download } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  getCategories,
  getBooks,
  toggleFavorite,
  getUserFavorites,
  type BookCategory,
  type Book,
} from '@/api/ebook'

const authStore = useAuthStore()

const sidebarCollapsed = ref(true)
const categories = ref<BookCategory[]>([])
const books = ref<Book[]>([])
const favoritedIds = ref<number[]>([])
const activeCategory = ref<number | null>(null)
const showFavorites = ref(false)
const keyword = ref('')
const initialLoading = ref(false)
const loadingMore = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)
const totalCount = ref(0)
const pageSize = 20
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const totalBooks = computed(() => categories.value.reduce((sum, c) => sum + c.count, 0))

onMounted(async () => {
  await fetchCategories()
  await loadBooks(true)
  if (authStore.isLoggedIn) {
    loadFavorites()
  }
  setupObserver()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})

watch(() => authStore.isLoggedIn, (val) => {
  if (val) loadFavorites()
  else favoritedIds.value = []
})

function setupObserver() {
  if (observer) observer.disconnect()
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && hasMore.value && !loadingMore.value) {
      loadMore()
    }
  }, { rootMargin: '200px' })
  nextTick(() => {
    if (sentinel.value) observer!.observe(sentinel.value)
  })
}

async function fetchCategories() {
  try {
    categories.value = await getCategories()
  } catch { /* ignore */ }
}

async function loadBooks(reset: boolean) {
  if (reset) {
    currentPage.value = 1
    hasMore.value = true
    initialLoading.value = true
    books.value = []
  }

  const params: Record<string, any> = {
    pageNum: currentPage.value,
    pageSize,
  }
  if (activeCategory.value != null) params.categoryId = activeCategory.value
  if (keyword.value) params.keyword = keyword.value
  if (showFavorites.value && favoritedIds.value.length > 0) {
    params.bookIds = favoritedIds.value.join(',')
  }

  try {
    const data = await getBooks(params as any)
    if (reset) {
      books.value = data.records
    } else {
      books.value = [...books.value, ...data.records]
    }
    totalCount.value = data.total
    hasMore.value = currentPage.value < data.pages
    currentPage.value = data.current
  } catch { /* ignore */ }
  finally {
    initialLoading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  currentPage.value++
  await loadBooks(false)
  nextTick(() => {
    if (sentinel.value) observer?.observe(sentinel.value)
  })
}

async function loadFavorites() {
  try {
    favoritedIds.value = await getUserFavorites()
  } catch { /* ignore */ }
}

function selectCategory(id: number | null) {
  showFavorites.value = false
  activeCategory.value = id
  loadBooks(true)
  nextTick(() => {
    if (sentinel.value) observer?.observe(sentinel.value)
  })
}

function doSearch() {
  loadBooks(true)
  nextTick(() => {
    if (sentinel.value) observer?.observe(sentinel.value)
  })
}

function toggleFavorites() {
  if (!authStore.isLoggedIn) { ElMessage({ message: '请先登录', type: 'warning', customClass: 'pink-msg' }); return }
  showFavorites.value = !showFavorites.value
  if (showFavorites.value) {
    activeCategory.value = null
  }
  loadBooks(true)
  nextTick(() => {
    if (sentinel.value) observer?.observe(sentinel.value)
  })
}

async function handleFavorite(book: Book) {
  if (!authStore.isLoggedIn) { ElMessage({ message: '请先登录', type: 'warning', customClass: 'pink-msg' }); return }
  try {
    const res = await toggleFavorite(book.id)
    if (res.favorited) {
      favoritedIds.value.push(book.id)
    } else {
      favoritedIds.value = favoritedIds.value.filter(id => id !== book.id)
    }
    if (showFavorites.value && !res.favorited) {
      books.value = books.value.filter(b => b.id !== book.id)
    }
  } catch { /* ignore */ }
}

async function handleDownload(book: Book) {
  if (!authStore.isLoggedIn) { ElMessage({ message: '请先登录', type: 'warning', customClass: 'pink-msg' }); return }
  const token = authStore.token
  if (!token) return
  const a = document.createElement('a')
  a.href = `/api/books/${book.id}/download-file?token=${encodeURIComponent(token)}`
  a.click()
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: $text-title;
  margin: 0 0 $gap-lg;
  padding-left: $gap-sm;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 4px;
    bottom: 4px;
    width: 4px;
    background: $border-dark;
    border-radius: 2px;
  }
}

.ebook-layout {
  display: flex;
  gap: $gap-lg;
  align-items: flex-start;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: $white;
  border: 1px solid $border;
  border-radius: $radius;
  padding: $gap-sm;
  transition: width $ease;

  &.collapsed {
    width: 44px;
  }
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  color: $text-muted;
  font-size: 14px;
  cursor: pointer;
  border-radius: $radius-sm;
  transition: color $ease;

  &:hover {
    color: $text-main;
  }
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  gap: $gap-sm;
  margin-top: $gap-xs;
}

.sidebar-upper {
  display: flex;
  flex-direction: column;
  gap: $gap-sm;
  overflow: hidden;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.35s ease;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
  max-height: 0;
  margin-bottom: -$gap-sm;
}

.category-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: calc(100vh - 420px);
  overflow-y: auto;

  li {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 10px;
    border-radius: $radius-sm;
    cursor: pointer;
    font-size: 14px;
    color: $text-body;
    transition: all $ease;

    &:hover {
      background: $bg-hover;
      color: $text-main;
    }

    &.active {
      background: $gray-100;
      color: $text-title;
      font-weight: 600;
    }
  }
}

.cat-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-count {
  font-size: 12px;
  color: $text-muted;
  flex-shrink: 0;
  margin-left: 8px;
}

.fav-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: $radius-sm;
  cursor: pointer;
  font-size: 14px;
  color: $text-body;
  border: 1px solid $border;
  transition: all $ease;
  margin-top: $gap-xs;

  &:hover {
    background: $bg-hover;
    color: $text-main;
  }

  &.active {
    background: $gray-100;
    color: #F56C6C;
    font-weight: 600;
    border-color: #F56C6C;
  }
}

.main-content {
  flex: 1;
  min-width: 0;
}

.result-bar {
  margin-bottom: $gap-sm;
}

.result-count {
  font-size: 14px;
  color: $text-sub;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $gap-sm;
}

.book-card {
  background: $white;
  border: 1px solid $border;
  border-radius: $radius;
  padding: $gap-sm;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 130px;
  transition: box-shadow $ease, border-color $ease;

  &:hover {
    border-color: $border-dark;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.book-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-main;
  line-height: 1.5;
  margin: 0 0 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-author {
  font-size: 12px;
  color: $text-muted;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: $gap-xs;

  .mr-4 {
    margin-right: 4px;
  }
}

.scroll-sentinel {
  display: flex;
  justify-content: center;
  padding: $gap-md;
  color: $text-muted;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .book-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .ebook-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;

    &.collapsed {
      width: 100%;
    }
  }

  .book-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .hero-title {
    font-size: 22px;
  }
}

@media (max-width: 480px) {
  .book-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
.pink-msg {
  background-color: #FFF0F5 !important;
  border-color: #FFB6C1 !important;
}

.pink-msg .el-message__content {
  color: #D63384 !important;
}

.pink-msg .el-message__icon {
  color: #FF69B4 !important;
}
</style>
