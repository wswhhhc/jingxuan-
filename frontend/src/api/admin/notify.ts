import request from '../request'

export interface NotifyItem {
  id: number
  title: string
  content: string
  isRead: number
  createTime: string
}

export function getNotifyList(params: { page?: number; size?: number; unreadOnly?: boolean }) {
  return request.get<{ records: NotifyItem[]; total: number }>('/admin/notify/list', { params })
}

export function markAsRead(id: number) {
  return request.post(`/admin/notify/read/${id}`)
}

export function markAllRead() {
  return request.post('/admin/notify/read-all')
}

export function deleteRead() {
  return request.delete('/admin/notify/read')
}

