import request from '@/utils/request'

export interface BookCategory {
  id: number
  name: string
  count: number
}

export interface Book {
  id: number
  categoryId: number
  title: string
  author: string
  downloadUrl: string
  createTime: string
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function getCategories(): Promise<BookCategory[]> {
  return request.get('/api/books/categories') as Promise<BookCategory[]>
}

export function getBooks(params: {
  pageNum: number
  pageSize: number
  categoryId?: number
  keyword?: string
}): Promise<PageData<Book>> {
  return request.get('/api/books', { params }) as Promise<PageData<Book>>
}

export function searchBooks(params: {
  pageNum: number
  pageSize: number
  keyword: string
}): Promise<PageData<Book>> {
  return request.get('/api/books/search', { params }) as Promise<PageData<Book>>
}

export function toggleFavorite(bookId: number): Promise<{ favorited: boolean }> {
  return request.post(`/api/books/${bookId}/favorite`) as Promise<{ favorited: boolean }>
}

export function getUserFavorites(): Promise<number[]> {
  return request.get('/api/user/favorites') as Promise<number[]>
}

export function recordDownload(bookId: number): Promise<void> {
  return request.post(`/api/books/${bookId}/download`) as Promise<void>
}
