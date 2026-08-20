<template>
  <div class="settings-page">
    <div class="page-header">
      <h1>站点设置</h1>
      <el-button type="primary" :loading="saving" @click="handleSave">保存全部</el-button>
    </div>

    <div class="settings-content">
      <!-- 频道管理（增删改） -->
      <el-card class="group-card">
        <template #header>
          <div class="group-header">
            <span class="group-title">频道名称</span>
            <el-button size="small" type="primary" @click="openAddChannel">
              <el-icon><Plus /></el-icon> 添加频道
            </el-button>
          </div>
        </template>
        <el-table :data="channelEdits" stripe size="small">
          <el-table-column label="路由路径" width="180">
            <template #default="{ row }">
              <el-input v-model="row.path" size="small" placeholder="/example" />
            </template>
          </el-table-column>
          <el-table-column label="显示名称" width="180">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" placeholder="频道名称" />
            </template>
          </el-table-column>
          <el-table-column label="图标名称" width="180">
            <template #default="{ row }">
              <el-input v-model="row.icon" size="small" placeholder="HomeFilled" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeChannel($index)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 其他设置分组 -->
      <el-card v-for="group in nonChannelGroups" :key="group.key" class="group-card">
        <template #header>
          <span class="group-title">{{ group.label }}</span>
        </template>
        <el-form label-width="140px" label-position="left" @submit.prevent>
          <el-form-item
            v-for="item in group.items"
            :key="item.settingKey"
            :label="item.settingKey"
          >
            <div class="form-item-wrap">
              <el-input
                v-if="item.settingType === 'text'"
                v-model="form[item.settingKey]"
                :placeholder="item.description"
              />
              <el-color-picker
                v-else-if="item.settingType === 'color'"
                v-model="form[item.settingKey]"
                show-alpha
              />
              <span class="field-desc">{{ item.description }}</span>
            </div>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 调度器监控 -->
      <el-card class="group-card">
        <template #header>
          <div class="group-header">
            <span class="group-title">调度器监控</span>
            <div style="display:flex;align-items:center;gap:8px">
              <el-tag :type="schedulerLoading ? 'warning' : 'success'" size="small">
                {{ schedulerLoading ? '刷新中...' : '自动刷新 10s' }}
              </el-tag>
              <el-button size="small" @click="loadSchedulers" :loading="schedulerLoading">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>
        </template>
        <el-table :data="schedulerTasks" stripe size="small" v-loading="schedulerLoading"
                  row-key="taskId" :expand-row-keys="expandedErrors"
                  @expand-change="onExpandChange">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="sched-error-msg">{{ row.errorMessage }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="任务名称" width="130" />
          <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
          <el-table-column prop="group" label="分组" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.group }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="触发" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="row.triggerType === 'CRON' ? 'warning' : 'info'">{{ row.triggerType }}</el-tag>
              <span class="trigger-val">{{ row.triggerValue }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTagType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上次运行" width="150">
            <template #default="{ row }">{{ formatSchedTime(row.lastStartTime) }}</template>
          </el-table-column>
          <el-table-column label="耗时" width="80">
            <template #default="{ row }">{{ row.duration || '-' }}</template>
          </el-table-column>
          <el-table-column label="成功/失败" width="90">
            <template #default="{ row }">
              <span class="cell-green">{{ row.executionCount - row.totalFailures }}</span>
              <span class="cell-divider">/</span>
              <span :class="row.totalFailures > 0 ? 'cell-red' : 'cell-green'">{{ row.totalFailures }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="handleTrigger(row)"
                :loading="row._triggering" :disabled="row.status === 'RUNNING'">执行</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Refresh } from '@element-plus/icons-vue'
import { getAdminSettings, updateAdminSettings, type SiteSetting } from '@/api/settings'
import { getSchedulerTasks, triggerSchedulerTask, type TaskStatus } from '@/api/monitor'
import type { Channel } from '@/stores/site'

const saving = ref(false)
const form = ref<Record<string, string>>({})
const rawSettings = ref<Record<string, SiteSetting[]>>({})

const groupLabels: Record<string, string> = {
  identity: '站点标识',
  module: '模块描述',
  contact: '联系/关于',
  email: '邮件/通知',
  upload: '上传规则',
  channel: '频道名称',
}

const groups = computed(() => {
  return Object.entries(rawSettings.value).map(([key, items]) => ({
    key,
    label: groupLabels[key] || key,
    items,
  }))
})

const nonChannelGroups = computed(() => {
  return groups.value.filter(g => g.key !== 'channel')
})

// --- 频道增删改 ---
const channelEdits = reactive<Channel[]>([])

function loadChannels() {
  const json = form.value['channels']
  try {
    channelEdits.splice(0, channelEdits.length, ...JSON.parse(json || '[]'))
  } catch {
    channelEdits.splice(0, channelEdits.length)
  }
}

function openAddChannel() {
  channelEdits.push({ path: '', name: '', icon: '' })
}

function removeChannel(index: number) {
  channelEdits.splice(index, 1)
}

// --- 生命周期 ---
let schedTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  try {
    const data = await getAdminSettings()
    rawSettings.value = data
    const flat: Record<string, string> = {}
    for (const items of Object.values(data)) {
      for (const item of items) {
        flat[item.settingKey] = item.settingValue || ''
      }
    }
    form.value = flat
    loadChannels()
  } catch {
    ElMessage.error('加载设置失败')
  }
  loadSchedulers()
  schedTimer = setInterval(loadSchedulers, 10000)
})

onUnmounted(() => {
  if (schedTimer) clearInterval(schedTimer)
})

async function handleSave() {
  saving.value = true
  try {
    // 将频道编辑合并到 form
    const payload = { ...form.value, channels: JSON.stringify(channelEdits) }
    await updateAdminSettings(payload)
    setTimeout(() => { window.location.href = '/' }, 400)
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// --- 调度器监控 ---
type SchedulerTask = TaskStatus & { _triggering?: boolean }
const schedulerTasks = ref<SchedulerTask[]>([])
const schedulerLoading = ref(false)
const expandedErrors = ref<string[]>([])

async function loadSchedulers() {
  schedulerLoading.value = true
  try {
    const list = await getSchedulerTasks()
    schedulerTasks.value = (list || []).map((t: TaskStatus) => ({ ...t, _triggering: false }))
  } catch { /* ignore */ }
  schedulerLoading.value = false
}

async function handleTrigger(row: SchedulerTask) {
  row._triggering = true
  try {
    await triggerSchedulerTask(row.taskId)
    ElMessage.success(`已触发: ${row.name}`)
    setTimeout(() => loadSchedulers(), 500)
  } catch (e: any) {
    ElMessage.error(e?.message || '触发失败')
  } finally {
    row._triggering = false
  }
}

function statusTagType(s: string) {
  if (s === 'RUNNING') return ''
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  return 'info'
}

function formatSchedTime(t: string | null) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

function onExpandChange(_row: SchedulerTask, rows: SchedulerTask[]) {
  expandedErrors.value = rows.map(r => r.taskId)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.settings-page {
  max-width: 900px;
  margin: 0 auto;
  padding: $gap-lg 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $gap-lg;

  h1 {
    font-size: 22px;
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

.settings-content {
  display: flex;
  flex-direction: column;
  gap: $gap-md;
}

.group-card {
  :deep(.el-card__header) {
    padding: 12px $gap-lg;
    background: $bg-section;
  }
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-main;
}

.form-item-wrap {
  display: flex;
  align-items: center;
  gap: $gap-sm;
  width: 100%;

  .el-input {
    flex: 1;
    max-width: 480px;
  }

  .field-desc {
    font-size: 12px;
    color: $text-muted;
    flex-shrink: 0;
  }
}

.trigger-val {
  margin-left: 4px;
  font-size: 12px;
  color: $text-muted;
}

.sched-error-msg {
  color: #F56C6C;
  font-size: 13px;
  padding: 4px 12px;
  line-height: 1.6;
}

.cell-green { color: #67C23A; font-weight: 500; }
.cell-red { color: #F56C6C; font-weight: 500; }
.cell-divider { color: $text-muted; margin: 0 1px; }
</style>
