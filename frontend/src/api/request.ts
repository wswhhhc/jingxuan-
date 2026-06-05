import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuthStorage as clearSharedAuthStorage, getAuthToken } from '@/utils/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

function extractMessage(payload: unknown): string | undefined {
  if (!payload) return undefined
  if (typeof payload === 'string') {
    try {
      const parsed = JSON.parse(payload)
      return extractMessage(parsed) || payload
    } catch {
      return payload
    }
  }
  if (typeof payload === 'object' && payload !== null) {
    const message = (payload as { message?: unknown }).message
    if (typeof message === 'string' && message.trim()) {
      return message
    }
  }
  return undefined
}

function getErrorMessage(error: any): string {
  return extractMessage(error?.response?.data)
    || extractMessage(error?.request?.responseText)
    || extractMessage(error?.response?.request?.responseText)
    || error?.message
    || '网络错误'
}

function isLoginRequest(config?: { url?: string | undefined } | null): boolean {
  return config?.url?.includes('/auth/login') ?? false
}

function clearAuthStorage() {
  clearSharedAuthStorage()
  localStorage.removeItem('remember')
}

request.interceptors.request.use(
  (config) => {
    const token = getAuthToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0 || res.code === 200) {
      return res
    }
    const loginRequest = isLoginRequest(response.config)
    if (!loginRequest) {
      ElMessage.error(res.message || '请求失败')
    }
    if (res.code === 401) {
      clearAuthStorage()
      if (!loginRequest) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(new Error(res.message || (loginRequest ? '账号或密码错误' : '请求失败')))
  },
  (error) => {
    const loginRequest = isLoginRequest(error?.config)
    const code = error?.response?.data?.code ?? error?.response?.status
    const fallbackMsg = loginRequest && code === 401 ? '账号或密码错误' : '网络错误'
    const msg = getErrorMessage(error) || fallbackMsg
    const finalMsg = loginRequest && code === 401 ? '账号或密码错误' : msg
    if (code === 401) {
      clearAuthStorage()
    }
    if (!loginRequest) {
      ElMessage.error(finalMsg)
    }
    return Promise.reject(new Error(finalMsg))
  }
)

export default request
