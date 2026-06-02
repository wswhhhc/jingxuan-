import request from '../request'

export interface RuntimeAdminItem {
  workId: number
  status: 'invalid' | 'prepared' | 'starting' | 'running' | 'failed' | 'stopped'
  previewUrl?: string | null
  backendPort?: number | null
  frontendPort?: number | null
  projectPath?: string | null
  lastAccessTime?: string | null
  errorMessage?: string | null
}

export function getRuntimeList() {
  return request.get<RuntimeAdminItem[]>('/runtime/admin/list')
}

export function stopRuntimeInstance(workId: number | string) {
  return request.post(`/runtime/${workId}/stop`)
}

export function prepareRuntimeInstance(workId: number | string) {
  return request.post(`/runtime/${workId}/prepare`)
}
