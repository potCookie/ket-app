import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// Prevent duplicate redirects
let redirecting = false
function clearAuthAndRedirect() {
  if (redirecting) return
  redirecting = true
  localStorage.removeItem('ket_token')
  localStorage.removeItem('ket_user')
  sessionStorage.setItem('login_tab', 'register')
  window.location.hash = '#/login'
}

// Request interceptor: attach JWT token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ket_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor
api.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result.code === 200) {
      return result.data
    }
    // "用户不存在" → session is stale, redirect to login/register
    if (result.message === '用户不存在') {
      clearAuthAndRedirect()
      return Promise.reject(new Error(result.message))
    }
    // Other business errors: just reject, let the caller handle display
    return Promise.reject(new Error(result.message || '请求失败'))
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络异常'
    // 401 → session expired
    if (error.response?.status === 401) {
      clearAuthAndRedirect()
      return Promise.reject(new Error(msg))
    }
    // "用户不存在" via error channel
    if (msg === '用户不存在') {
      clearAuthAndRedirect()
      return Promise.reject(new Error(msg))
    }
    // Network errors: show a brief message
    if (error.message === 'Network Error' || error.code === 'ERR_NETWORK') {
      if (!redirecting) ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(new Error(msg))
  }
)

export default api
