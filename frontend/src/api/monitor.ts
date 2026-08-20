import request from '@/utils/request'

export interface JvmMetrics {
  heapUsed: string
  heapMax: string
  heapUsedPercent: number
  nonHeapUsed: string
  threadCount: number
  peakThreadCount: number
  gcCount: number
  gcTimeMs: number
  availableProcessors: number
  systemLoad: number | string
  uptime: string
}

export interface RedisMetrics {
  usedMemory: string
  connectedClients: number
  keyspaceHits: number
  keyspaceMisses: number
  hitRate: number
  totalKeys: number | string
  uptime: string
  error?: string
}

export interface DbMetrics {
  activeConnections: number
  idleConnections: number
  totalConnections: number
  waitingThreads: number
  error?: string
}

export interface CacheStats {
  hitCount: number
  missCount: number
  hitRate: number
  evictionCount: number
  loadCount: number
  estimatedSize: number
}

export interface SystemMetrics {
  osName: string
  osArch: string
  javaVersion: string
  javaVendor: string
  totalDiskSpace: string
  freeDiskSpace: string
  blogCount: number
  userCount: number
  diskError?: string
}

export interface MonitorData {
  jvm: JvmMetrics
  redis: RedisMetrics
  database: DbMetrics
  cache: Record<string, CacheStats>
  system: SystemMetrics
}

export function getMonitorData(): Promise<MonitorData> {
  return request.get('/api/admin/monitor') as Promise<MonitorData>
}

export interface TaskStatus {
  taskId: string
  name: string
  description: string
  group: string
  triggerType: string
  triggerValue: string
  status: 'IDLE' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  lastStartTime: string | null
  lastEndTime: string | null
  duration: string | null
  errorMessage: string | null
  executionCount: number
  totalFailures: number
}

export function getSchedulerTasks(): Promise<TaskStatus[]> {
  return request.get('/api/admin/monitor/schedulers')
}

export function triggerSchedulerTask(taskId: string): Promise<void> {
  return request.post(`/api/admin/monitor/scheduler/${taskId}/trigger`)
}
