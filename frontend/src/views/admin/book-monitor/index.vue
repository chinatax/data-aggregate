<template>
  <div class="monitor-page">
    <div class="page-header">
      <h2>电子书监控</h2>
      <el-tag v-if="latest" :type="statusType" size="small">{{ statusText }}</el-tag>
    </div>

    <!-- 最近扫描概览 -->
    <el-card shadow="never" class="monitor-card">
      <template #header><span class="card-title">最近扫描</span></template>
      <div v-if="latest" class="summary-grid">
        <div class="summary-item">
          <span class="summary-label">扫描时间</span>
          <span class="summary-value">{{ formatTime(latest.scanTime) }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">耗时</span>
          <span class="summary-value">{{ duration }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">状态</span>
          <el-tag :type="statusType" size="small">{{ latest.status }}</el-tag>
        </div>
      </div>
      <el-empty v-else description="暂无监控记录" :image-size="60" />
    </el-card>

    <!-- 变更统计 -->
    <el-card shadow="never" class="monitor-card" v-if="latest">
      <template #header><span class="card-title">变更统计</span></template>
      <div class="stats-row">
        <div class="stat-group">
          <div class="stat-title">文件</div>
          <div class="stat-tags">
            <el-tag type="success" size="small">新增 {{ latest.filesAdded }}</el-tag>
            <el-tag type="warning" size="small">修改 {{ latest.filesModified }}</el-tag>
            <el-tag type="danger" size="small">删除 {{ latest.filesDeleted }}</el-tag>
          </div>
        </div>
        <div class="stat-group">
          <div class="stat-title">书籍</div>
          <div class="stat-tags">
            <el-tag type="success" size="small">新增 {{ latest.booksAdded }}</el-tag>
            <el-tag type="warning" size="small">更新 {{ latest.booksUpdated }}</el-tag>
            <el-tag type="danger" size="small">移除 {{ latest.booksRemoved }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 错误详情 -->
    <el-card shadow="never" class="monitor-card" v-if="latest && latest.errorMessage">
      <template #header><span class="card-title" style="color:#F56C6C">错误详情</span></template>
      <ul class="error-list">
        <li v-for="(err, i) in parseErrors(latest.errorMessage)" :key="i">{{ err }}</li>
      </ul>
    </el-card>

    <!-- 扫描历史 -->
    <el-card shadow="never" class="monitor-card">
      <template #header><span class="card-title">扫描历史</span></template>
      <el-table :data="history" stripe size="small" v-loading="historyLoading">
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
            {{ row.endTime ? calcDuration(row.scanTime, row.endTime) : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="historyTotal > pageSize">
        <el-pagination
          v-model:current-page="historyPage"
          :page-size="pageSize"
          :total="historyTotal"
          layout="prev, pager, next"
          size="small"
          @current-change="fetchHistory"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getLatestMonitorLog, getMonitorHistory, type BookMonitorLog } from '@/api/bookMonitor'

const latest = ref<BookMonitorLog | null>(null)
const history = ref<BookMonitorLog[]>([])
const historyLoading = ref(false)
const historyPage = ref(1)
const historyTotal = ref(0)
const pageSize = 20

const statusType = computed(() => {
  if (!latest.value) return 'info'
  if (latest.value.status === 'SUCCESS') return 'success'
  if (latest.value.status === 'PARTIAL') return 'warning'
  return 'danger'
})

const statusText = computed(() => {
  if (!latest.value) return ''
  if (latest.value.status === 'SUCCESS') return '运行正常'
  if (latest.value.status === 'PARTIAL') return '部分异常'
  return '运行失败'
})

const duration = computed(() => {
  if (!latest.value || !latest.value.endTime) return '-'
  return calcDuration(latest.value.scanTime, latest.value.endTime)
})

onMounted(async () => {
  try {
    latest.value = await getLatestMonitorLog()
  } catch { /* ignore */ }
  fetchHistory()
})

async function fetchHistory() {
  historyLoading.value = true
  try {
    const data = await getMonitorHistory({ pageNum: historyPage.value, pageSize })
    history.value = data.records
    historyTotal.value = data.total
  } catch { /* ignore */ }
  finally { historyLoading.value = false }
}

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

function calcDuration(start: string, end: string) {
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
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.monitor-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 $gap-md $gap-xl;
}

.page-header {
  display: flex;
  align-items: center;
  gap: $gap-sm;
  margin-bottom: $gap-lg;

  h2 {
    font-size: 20px;
    font-weight: 700;
    color: $text-title;
    margin: 0;
  }
}

.monitor-card {
  margin-bottom: $gap-md;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-title;
}

.summary-grid {
  display: flex;
  gap: $gap-xl;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: $text-muted;
}

.summary-value {
  font-size: 14px;
  color: $text-main;
}

.stats-row {
  display: flex;
  gap: $gap-xl;
}

.stat-group {
  display: flex;
  flex-direction: column;
  gap: $gap-xs;
}

.stat-title {
  font-size: 13px;
  color: $text-sub;
  font-weight: 600;
}

.stat-tags {
  display: flex;
  gap: 8px;
}

.error-list {
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
