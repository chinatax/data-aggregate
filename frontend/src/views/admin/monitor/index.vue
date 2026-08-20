<template>
  <div class="monitor-page">
    <div class="page-header">
      <h2>性能监控</h2>
      <el-tag :type="refreshing ? 'warning' : 'success'" size="small">
        {{ refreshing ? '刷新中...' : '自动刷新 10s' }}
      </el-tag>
    </div>

    <!-- 顶部：JVM + 系统 -->
    <div class="card-grid cols-3">
      <!-- JVM 内存 -->
      <el-card shadow="never">
        <template #header><span class="card-title">JVM 内存</span></template>
        <div class="mem-section">
          <div class="mem-ring">
            <el-progress type="dashboard" :percentage="data.jvm.heapUsedPercent" :color="heapColor" :stroke-width="12">
              <template #default="{ percentage }">
                <span class="ring-value">{{ percentage }}%</span>
              </template>
            </el-progress>
            <span class="ring-label">堆内存使用率</span>
          </div>
          <div class="mem-list">
            <div class="mem-item">
              <span class="mem-key">已用</span>
              <span class="mem-val">{{ data.jvm.heapUsed }}</span>
            </div>
            <div class="mem-item">
              <span class="mem-key">最大</span>
              <span class="mem-val">{{ data.jvm.heapMax }}</span>
            </div>
            <div class="mem-item">
              <span class="mem-key">非堆</span>
              <span class="mem-val">{{ data.jvm.nonHeapUsed }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 线程 & GC -->
      <el-card shadow="never">
        <template #header><span class="card-title">线程 & GC</span></template>
        <div class="stats-rows">
          <div class="stat-row">
            <span class="stat-label">活动线程</span>
            <span class="stat-value">{{ data.jvm.threadCount }}</span>
            <span class="stat-sub">峰值 {{ data.jvm.peakThreadCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">GC 次数</span>
            <span class="stat-value">{{ data.jvm.gcCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">GC 耗时</span>
            <span class="stat-value">{{ fmtMs(data.jvm.gcTimeMs) }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">可用核心</span>
            <span class="stat-value">{{ data.jvm.availableProcessors }}</span>
            <span class="stat-sub">负载 {{ data.jvm.systemLoad }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">运行时间</span>
            <span class="stat-value mono">{{ data.jvm.uptime }}</span>
          </div>
        </div>
      </el-card>

      <!-- 系统信息 -->
      <el-card shadow="never">
        <template #header><span class="card-title">系统信息</span></template>
        <div class="stats-rows">
          <div class="stat-row">
            <span class="stat-label">OS</span>
            <span class="stat-value mono">{{ data.system.osName }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">Java</span>
            <span class="stat-value mono">{{ data.system.javaVersion }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">磁盘总量</span>
            <span class="stat-value">{{ data.system.totalDiskSpace }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">磁盘可用</span>
            <span class="stat-value" :class="{ 'text-warn': diskWarn }">{{ data.system.freeDiskSpace }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">博客数</span>
            <span class="stat-value">{{ data.system.blogCount }}</span>
            <span class="stat-sub">用户 {{ data.system.userCount }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 底部：Redis + DB + Cache -->
    <div class="card-grid cols-3">
      <!-- Redis -->
      <el-card shadow="never">
        <template #header>
          <span class="card-title">Redis</span>
          <el-tag v-if="data.redis.error" type="danger" size="small" effect="plain">异常</el-tag>
        </template>
        <div class="stats-rows">
          <div class="stat-row">
            <span class="stat-label">已用内存</span>
            <span class="stat-value">{{ data.redis.usedMemory }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">连接客户端</span>
            <span class="stat-value">{{ data.redis.connectedClients }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">命中率</span>
            <span class="stat-value" :class="hitRateClass(data.redis.hitRate)">{{ data.redis.hitRate }}%</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">命中 / 未命中</span>
            <span class="stat-value mono">{{ data.redis.keyspaceHits }} / {{ data.redis.keyspaceMisses }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">缓存 Key 数</span>
            <span class="stat-value">{{ data.redis.totalKeys }}</span>
          </div>
        </div>
      </el-card>

      <!-- 数据库连接池 -->
      <el-card shadow="never">
        <template #header>
          <span class="card-title">数据库连接池</span>
          <el-tag v-if="data.database.error" type="danger" size="small" effect="plain">异常</el-tag>
        </template>
        <div class="db-pool">
          <div class="pool-bars">
            <div class="pool-bar-item">
              <div class="pool-bar-label">活跃</div>
              <div class="pool-bar-track">
                <div class="pool-bar-fill active" :style="{ width: poolPct(data.database.activeConnections, data.database.totalConnections) }"></div>
              </div>
              <span class="pool-bar-num">{{ data.database.activeConnections }}</span>
            </div>
            <div class="pool-bar-item">
              <div class="pool-bar-label">空闲</div>
              <div class="pool-bar-track">
                <div class="pool-bar-fill idle" :style="{ width: poolPct(data.database.idleConnections, data.database.totalConnections) }"></div>
              </div>
              <span class="pool-bar-num">{{ data.database.idleConnections }}</span>
            </div>
            <div class="pool-bar-item">
              <div class="pool-bar-label">等待</div>
              <div class="pool-bar-track">
                <div class="pool-bar-fill waiting" :style="{ width: poolPct(data.database.waitingThreads, data.database.totalConnections) }"></div>
              </div>
              <span class="pool-bar-num" :class="{ 'text-danger': data.database.waitingThreads > 0 }">{{ data.database.waitingThreads }}</span>
            </div>
          </div>
          <div class="pool-total">总连接数: {{ data.database.totalConnections }}</div>
        </div>
      </el-card>

      <!-- L1 缓存统计 -->
      <el-card shadow="never">
        <template #header><span class="card-title">L1 缓存 (Caffeine)</span></template>
        <div class="cache-stats">
          <div v-for="(stats, name) in data.cache" :key="name" class="cache-row">
            <div class="cache-name">{{ name }}</div>
            <div class="cache-bar-track">
              <div class="cache-bar-fill" :style="{ width: stats.hitRate + '%' }" :class="cacheBarClass(stats.hitRate)"></div>
            </div>
            <div class="cache-rate" :class="cacheRateClass(stats.hitRate)">{{ stats.hitRate }}%</div>
            <div class="cache-info">{{ stats.hitCount }}/{{ stats.hitCount + stats.missCount }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 电子书监控 -->
    <div class="section-header">
      <h2>电子书监控</h2>
      <el-tag v-if="bookLatest" :type="bookStatusType" size="small">{{ bookStatusText }}</el-tag>
    </div>

    <div class="card-grid cols-3">
      <!-- 最近扫描概览 -->
      <el-card shadow="never">
        <template #header><span class="card-title">最近扫描</span></template>
        <div v-if="bookLatest" class="stats-rows">
          <div class="stat-row">
            <span class="stat-label">扫描时间</span>
            <span class="stat-value mono">{{ formatTime(bookLatest.scanTime) }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">耗时</span>
            <span class="stat-value">{{ bookDuration }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">状态</span>
            <el-tag :type="bookStatusType" size="small">{{ bookLatest.status }}</el-tag>
          </div>
        </div>
        <el-empty v-else description="暂无监控记录" :image-size="60" />
      </el-card>

      <!-- 变更统计 -->
      <el-card shadow="never">
        <template #header><span class="card-title">变更统计</span></template>
        <div v-if="bookLatest" class="stats-rows">
          <div class="stat-row">
            <span class="stat-label">文件</span>
            <span class="stat-value">
              <el-tag type="success" size="small">+{{ bookLatest.filesAdded }}</el-tag>
              <el-tag type="warning" size="small" style="margin-left:4px">~{{ bookLatest.filesModified }}</el-tag>
              <el-tag type="danger" size="small" style="margin-left:4px">-{{ bookLatest.filesDeleted }}</el-tag>
            </span>
          </div>
          <div class="stat-row">
            <span class="stat-label">书籍</span>
            <span class="stat-value">
              <el-tag type="success" size="small">+{{ bookLatest.booksAdded }}</el-tag>
              <el-tag type="warning" size="small" style="margin-left:4px">~{{ bookLatest.booksUpdated }}</el-tag>
              <el-tag type="danger" size="small" style="margin-left:4px">-{{ bookLatest.booksRemoved }}</el-tag>
            </span>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </el-card>

      <!-- 错误详情 -->
      <el-card shadow="never" v-if="bookLatest && bookLatest.errorMessage">
        <template #header><span class="card-title" style="color:#F56C6C">错误详情</span></template>
        <ul class="book-error-list">
          <li v-for="(err, i) in parseErrors(bookLatest.errorMessage)" :key="i">{{ err }}</li>
        </ul>
      </el-card>
    </div>

    <!-- 扫描历史 -->
    <el-card shadow="never" class="book-history-card">
      <template #header><span class="card-title">扫描历史</span></template>
      <el-table :data="bookHistory" stripe size="small" v-loading="bookHistoryLoading">
        <el-table-column prop="scanTime" label="扫描时间" width="170">
          <template #default="{ row }">{{ formatTime(row.scanTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'PARTIAL' ? 'warning' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件增/改/删" width="130">
          <template #default="{ row }">
            <span class="cell-green">+{{ row.filesAdded }}</span>
            <span class="cell-divider">/</span>
            <span class="cell-orange">~{{ row.filesModified }}</span>
            <span class="cell-divider">/</span>
            <span class="cell-red">-{{ row.filesDeleted }}</span>
          </template>
        </el-table-column>
        <el-table-column label="书籍增/更/删" width="130">
          <template #default="{ row }">
            <span class="cell-green">+{{ row.booksAdded }}</span>
            <span class="cell-divider">/</span>
            <span class="cell-orange">~{{ row.booksUpdated }}</span>
            <span class="cell-divider">/</span>
            <span class="cell-red">-{{ row.booksRemoved }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ row.endTime ? calcBookDuration(row.scanTime, row.endTime) : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="bookHistoryTotal > bookPageSize">
        <el-pagination
          v-model:current-page="bookHistoryPage"
          :page-size="bookPageSize"
          :total="bookHistoryTotal"
          layout="prev, pager, next"
          size="small"
          @current-change="fetchBookHistory"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getMonitorData, type MonitorData } from '@/api/monitor'
import { getLatestMonitorLog, getMonitorHistory, type BookMonitorLog } from '@/api/bookMonitor'

const data = ref<MonitorData>({
  jvm: { heapUsed: '0', heapMax: '0', heapUsedPercent: 0, nonHeapUsed: '0', threadCount: 0, peakThreadCount: 0, gcCount: 0, gcTimeMs: 0, availableProcessors: 0, systemLoad: 'N/A', uptime: '' },
  redis: { usedMemory: 'N/A', connectedClients: 0, keyspaceHits: 0, keyspaceMisses: 0, hitRate: 0, totalKeys: 0, uptime: '' },
  database: { activeConnections: 0, idleConnections: 0, totalConnections: 0, waitingThreads: 0 },
  cache: {},
  system: { osName: '', osArch: '', javaVersion: '', javaVendor: '', totalDiskSpace: '', freeDiskSpace: '', blogCount: 0, userCount: 0 },
})

const refreshing = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

async function load() {
  refreshing.value = true
  try {
    const res = await getMonitorData()
    if (res) data.value = res
  } catch { /* ignore */ }
  refreshing.value = false
}

const heapColor = computed(() => {
  const p = data.value.jvm.heapUsedPercent
  if (p > 85) return '#F56C6C'
  if (p > 65) return '#E6A23C'
  return '#67C23A'
})

const diskWarn = computed(() => {
  const free = data.value.system.freeDiskSpace
  const total = data.value.system.totalDiskSpace
  if (!free || !total) return false
  const parseGb = (s: string) => parseFloat(s.replace(/[^0-9.]/g, '')) * (s.includes('MB') ? 0.001 : s.includes('KB') ? 0.000001 : s.includes('TB') ? 1000 : 1)
  return parseGb(free) / parseGb(total) < 0.1
})

function hitRateClass(rate: number) {
  if (rate >= 90) return 'text-success'
  if (rate >= 70) return 'text-warn'
  return 'text-danger'
}

function cacheBarClass(rate: number) {
  if (rate >= 90) return 'good'
  if (rate >= 70) return 'warn'
  return 'bad'
}

function cacheRateClass(rate: number) {
  if (rate >= 90) return 'text-success'
  if (rate >= 70) return 'text-warn'
  return 'text-danger'
}

function poolPct(val: number, total: number) {
  return total > 0 ? (val / total * 100).toFixed(0) + '%' : '0%'
}

function fmtMs(ms: number) {
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

// ====== 电子书监控 ======
const bookLatest = ref<BookMonitorLog | null>(null)
const bookHistory = ref<BookMonitorLog[]>([])
const bookHistoryLoading = ref(false)
const bookHistoryPage = ref(1)
const bookHistoryTotal = ref(0)
const bookPageSize = 20

const bookStatusType = computed(() => {
  if (!bookLatest.value) return 'info'
  if (bookLatest.value.status === 'SUCCESS') return 'success'
  if (bookLatest.value.status === 'PARTIAL') return 'warning'
  return 'danger'
})

const bookStatusText = computed(() => {
  if (!bookLatest.value) return ''
  if (bookLatest.value.status === 'SUCCESS') return '运行正常'
  if (bookLatest.value.status === 'PARTIAL') return '部分异常'
  return '运行失败'
})

const bookDuration = computed(() => {
  if (!bookLatest.value || !bookLatest.value.endTime) return '-'
  return calcBookDuration(bookLatest.value.scanTime, bookLatest.value.endTime)
})

async function loadBookMonitor() {
  try {
    bookLatest.value = await getLatestMonitorLog()
  } catch { /* ignore */ }
  fetchBookHistory()
}

async function fetchBookHistory() {
  bookHistoryLoading.value = true
  try {
    const res = await getMonitorHistory({ pageNum: bookHistoryPage.value, pageSize: bookPageSize })
    bookHistory.value = res.records
    bookHistoryTotal.value = res.total
  } catch { /* ignore */ }
  finally { bookHistoryLoading.value = false }
}

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

function calcBookDuration(start: string, end: string) {
  const s = new Date(start).getTime()
  const e = new Date(end).getTime()
  const diff = (e - s) / 1000
  if (diff < 60) return diff.toFixed(1) + 's'
  return (diff / 60).toFixed(1) + 'min'
}

function parseErrors(msg: string): string[] {
  try {
    return JSON.parse(msg)
  } catch {
    return [msg]
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 10000)
  loadBookMonitor()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.monitor-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: $gap-lg;
}

.page-header {
  display: flex;
  align-items: center;
  gap: $gap-md;
  margin-bottom: $gap-lg;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: $text-main;
    position: relative;
    padding-left: $gap-sm;

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
}

.card-grid {
  display: grid;
  gap: $gap-lg;
  margin-bottom: $gap-lg;

  &.cols-3 {
    grid-template-columns: repeat(3, 1fr);
  }
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-main;
}

/* JVM Memory */
.mem-section {
  display: flex;
  align-items: center;
  gap: $gap-lg;
}

.mem-ring {
  display: flex;
  flex-direction: column;
  align-items: center;

  .ring-value {
    font-size: 18px;
    font-weight: 700;
  }

  .ring-label {
    font-size: 12px;
    color: $gray-400;
    margin-top: 4px;
  }
}

.mem-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mem-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;

  .mem-key { color: $gray-400; }
  .mem-val { color: $text-main; font-weight: 500; }
}

/* Stats rows */
.stats-rows {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stat-row {
  display: flex;
  align-items: baseline;
  gap: 8px;

  .stat-label {
    font-size: 13px;
    color: $gray-400;
    min-width: 70px;
  }

  .stat-value {
    font-size: 15px;
    font-weight: 600;
    color: $text-main;
  }

  .stat-sub {
    font-size: 12px;
    color: $gray-300;
    margin-left: auto;
  }

  .mono {
    font-family: 'SFMono-Regular', monospace;
    font-size: 13px;
  }
}

/* DB Pool bars */
.db-pool {
  .pool-bars {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 14px;
  }

  .pool-bar-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .pool-bar-label {
    font-size: 12px;
    color: $gray-400;
    width: 32px;
  }

  .pool-bar-track {
    flex: 1;
    height: 8px;
    background: $gray-100;
    border-radius: 4px;
    overflow: hidden;
  }

  .pool-bar-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.5s ease;

    &.active { background: #67C23A; }
    &.idle { background: #909399; }
    &.waiting { background: #F56C6C; }
  }

  .pool-bar-num {
    font-size: 14px;
    font-weight: 600;
    width: 24px;
    text-align: right;
  }

  .pool-total {
    font-size: 13px;
    color: $gray-400;
    text-align: center;
  }
}

/* Cache stats */
.cache-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cache-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cache-name {
  font-size: 12px;
  color: $text-sub;
  width: 72px;
  font-family: 'SFMono-Regular', monospace;
}

.cache-bar-track {
  flex: 1;
  height: 6px;
  background: $gray-100;
  border-radius: 3px;
  overflow: hidden;
}

.cache-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;

  &.good { background: #67C23A; }
  &.warn { background: #E6A23C; }
  &.bad { background: #F56C6C; }
}

.cache-rate {
  font-size: 13px;
  font-weight: 600;
  width: 40px;
  text-align: right;
}

.cache-info {
  font-size: 11px;
  color: $gray-300;
  width: 48px;
  text-align: right;
}

/* Color utils */
.text-success { color: #67C23A; }
.text-warn { color: #E6A23C; }
.text-danger { color: #F56C6C; }

/* Book monitor section */
.section-header {
  display: flex;
  align-items: center;
  gap: $gap-md;
  margin-bottom: $gap-lg;
  margin-top: $gap-xs;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: $text-main;
    position: relative;
    padding-left: $gap-sm;

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
}

.book-history-card {
  margin-top: $gap-lg;
}

.book-error-list {
  margin: 0;
  padding-left: 20px;
  color: #F56C6C;
  font-size: 13px;
  line-height: 1.8;
}

.cell-green { color: #67C23A; }
.cell-orange { color: #E6A23C; }
.cell-red { color: #F56C6C; }
.cell-divider { color: $text-muted; margin: 0 2px; }

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: $gap-sm;
}
</style>
