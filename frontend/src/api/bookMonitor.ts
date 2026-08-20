import request from '@/utils/request'

export interface BookMonitorLog {
  id: number
  scanTime: string
  endTime: string
  filesAdded: number
  filesModified: number
  filesDeleted: number
  booksAdded: number
  booksUpdated: number
  booksRemoved: number
  errorMessage: string | null
  status: 'SUCCESS' | 'PARTIAL' | 'FAILED'
  details: string | null
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function getLatestMonitorLog(): Promise<BookMonitorLog> {
  return request.get('/api/admin/book-monitor/latest') as Promise<BookMonitorLog>
}

export function getMonitorHistory(params: {
  pageNum: number
  pageSize: number
}): Promise<PageData<BookMonitorLog>> {
  return request.get('/api/admin/book-monitor/history', { params }) as Promise<PageData<BookMonitorLog>>
}
