import request from '@/utils/request'

export interface SiteSetting {
  settingKey: string
  settingValue: string
  settingType: string
  groupName: string
  description: string
  sortOrder: number
}

export function getAdminSettings(): Promise<Record<string, SiteSetting[]>> {
  return request.get('/api/admin/settings') as Promise<Record<string, SiteSetting[]>>
}

export function updateAdminSettings(data: Record<string, string>): Promise<void> {
  return request.put('/api/admin/settings', data) as Promise<void>
}

export function getSiteConfig(): Promise<Record<string, string>> {
  return request.get('/api/site/config') as Promise<Record<string, string>>
}
