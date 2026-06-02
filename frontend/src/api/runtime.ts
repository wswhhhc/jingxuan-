import request from './request'

export interface PrepareRuntimeResponse {
  status: 'prepared' | 'invalid'
  valid: boolean
  message: string
  projectPath?: string
  manifestPath?: string
  missingFields?: string[]
  missingFiles?: string[]
}

export interface StartRuntimeResponse {
  status: 'starting' | 'running' | 'failed'
  message?: string
  previewUrl?: string
  backendPort?: number
  frontendPort?: number
  startedAt?: string
}

export interface RuntimeStatusResponse {
  workId: number
  status: 'invalid' | 'prepared' | 'starting' | 'running' | 'failed' | 'stopped'
  message?: string
  previewUrl?: string
  backendPort?: number
  frontendPort?: number
  backendReady?: boolean
  frontendReady?: boolean
  lastAccessTime?: string
  startTime?: string
  stopTime?: string
  errorMessage?: string
}

export function prepareRuntime(workId: string | number) {
  return request.post<PrepareRuntimeResponse>(`/runtime/${workId}/prepare`)
}

export function startRuntime(workId: string | number) {
  return request.post<StartRuntimeResponse>(`/runtime/${workId}/start`)
}

export function getRuntimeStatus(workId: string | number) {
  return request.get<RuntimeStatusResponse>(`/runtime/${workId}/status`)
}

export function heartbeatRuntime(workId: string | number) {
  return request.post<RuntimeStatusResponse>(`/runtime/${workId}/heartbeat`)
}

export function stopRuntime(workId: string | number) {
  return request.post<RuntimeStatusResponse>(`/runtime/${workId}/stop`)
}
