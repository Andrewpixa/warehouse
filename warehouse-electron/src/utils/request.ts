import axios from 'axios'
import { ElMessage } from 'element-plus'
import { PERM_KEY } from './perm'
import { DEFAULT_API_BASE, normalizeApiBase } from './config'

export const TOKEN_KEY = 'employee_satoken'
export const USER_KEY = 'employee_user_name'
export const API_BASE_KEY = 'employee_api_base'
export { PERM_KEY }

const service = axios.create({
  baseURL: import.meta.env.DEV ? '/warehouse' : DEFAULT_API_BASE,
  timeout: 15000
})

export function getApiBase(): string {
  return String(service.defaults.baseURL || DEFAULT_API_BASE)
}

export function applyApiBase(apiBase: string) {
  const next = import.meta.env.DEV ? '/warehouse' : normalizeApiBase(apiBase)
  service.defaults.baseURL = next
  if (!import.meta.env.DEV) {
    localStorage.setItem(API_BASE_KEY, next)
  }
  return next
}

export async function initApiBase() {
  if (import.meta.env.DEV) {
    applyApiBase('/warehouse')
    return getApiBase()
  }
  const fromElectron = await window.employeeApp?.getConfig?.()
  const saved = fromElectron?.apiBase || localStorage.getItem(API_BASE_KEY) || DEFAULT_API_BASE
  return applyApiBase(saved)
}

export async function saveApiBase(apiBase: string) {
  const next = applyApiBase(apiBase)
  if (window.employeeApp?.setConfig) {
    await window.employeeApp.setConfig({ apiBase: next })
  }
  return next
}

export const BASE_URL: string = service.defaults.baseURL as string

service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers = config.headers || {}
    config.headers.satoken = token
  }
  const contentType = config.headers?.['Content-Type']
  const isJsonRequest = typeof contentType === 'string' && contentType.includes('application/json')
  if (config.method === 'post' && config.data
    && !isJsonRequest
    && !(config.data instanceof FormData)
    && !(config.data instanceof URLSearchParams)) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(config.data)) {
      if (value !== null && value !== undefined) {
        params.append(key, String(value))
      }
    }
    config.data = params
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === -1) {
      ElMessage.error(res.msg || '操作失败')
      return Promise.reject(new Error(res.msg || '操作失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(PERM_KEY)
      ElMessage.error('登录已过期，请重新登录')
      window.location.reload()
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
