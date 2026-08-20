import request from '@/utils/request'

export interface CategoryItem {
  id: number
  code: string
  name: string
}

export function getCategories(): Promise<CategoryItem[]> {
  return request.get('/api/categories', { params: { _t: Date.now() } }) as Promise<CategoryItem[]>
}

export function createCategory(data: { code: string; name: string }): Promise<void> {
  return request.post('/api/admin/categories', data) as Promise<void>
}

export function updateCategory(id: number, data: { name: string }): Promise<void> {
  return request.put(`/api/admin/categories/${id}`, data) as Promise<void>
}

export function deleteCategory(id: number): Promise<void> {
  return request.delete(`/api/admin/categories/${id}`) as Promise<void>
}

export function moveCategoryUp(id: number): Promise<void> {
  return request.put(`/api/admin/categories/${id}/move-up`) as Promise<void>
}

export function moveCategoryDown(id: number): Promise<void> {
  return request.put(`/api/admin/categories/${id}/move-down`) as Promise<void>
}
