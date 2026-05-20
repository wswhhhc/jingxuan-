import request from '../request'

export interface PortItem {
  id: number
  portNumber: number
  status: 'free' | 'in_use'
  workId?: number | null
  workTitle?: string | null
  allocatedTime?: string | null
  proxyUrl?: string | null
}

export function getPortList(params: { page?: number; size?: number; status?: string }) {
  return request.get('/admin/port/list', { params })
}

export function allocatePort(data: { workId: number; portNumber: number }) {
  return request.post('/admin/port/allocate', data)
}

export function releasePort(id: number) {
  return request.post(`/admin/port/${id}/release`)
}

export function getAvailablePorts() {
  return request.get('/admin/port/available')
}
