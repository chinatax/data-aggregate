import request from '@/utils/request'

export interface CommentItem {
  id: number
  blogId: number
  parentId: number | null
  authorName: string
  content: string
  createTime: string
  children?: CommentItem[]
}

export function getComments(blogId: number): Promise<CommentItem[]> {
  return request.get(`/api/blog/${blogId}/comments`) as Promise<CommentItem[]>
}

export function addComment(blogId: number, data: { authorName: string; content: string }): Promise<CommentItem> {
  return request.post(`/api/blog/${blogId}/comments`, data) as Promise<CommentItem>
}

export function addReply(commentId: number, data: { authorName: string; content: string }): Promise<CommentItem> {
  return request.post(`/api/comments/${commentId}/reply`, data) as Promise<CommentItem>
}
