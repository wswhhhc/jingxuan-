import request from '../request'

export interface NotifyItem {
  id: number
  title: string
  content: string
  isRead: number
  createTime: string
}

export function getNotifyList(params: { page?: number; size?: number; unreadOnly?: boolean }) {
  return request.get<{ records: NotifyItem[]; total: number }>('/student/notify/list', { params })
}

export function markAsRead(id: number) {
  return request.post(`/student/notify/read/${id}`)
}

export function markAllRead() {
  return request.post('/student/notify/read-all')
}

export function getUnreadCount() {
  return request.get<{ count: number }>('/student/notify/unread-count')
}

export function deleteRead() {
  return request.delete('/student/notify/read')
}

