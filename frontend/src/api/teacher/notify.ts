import request from '../request'

export interface NotifyItem {
  id: number
  title: string
  content: string
  isRead: number
  createTime: string
}

export function getNotifyList(params: { page?: number; size?: number; unreadOnly?: boolean }) {
  return request.get<{ records: NotifyItem[]; total: number }>('/teacher/notify/list', { params })
}

export function markAsRead(id: number) {
  return request.post(`/teacher/notify/read/${id}`)
}

export function markAllRead() {
  return request.post('/teacher/notify/read-all')
}

export function getUnreadCount() {
  return request.get<{ count: number }>('/teacher/notify/unread-count')
}
