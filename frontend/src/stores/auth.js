import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, getMe } from '../api/auth.js'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('ket_token') || null)

  const isLoggedIn = computed(() => !!token.value)
  const isChild = computed(() => user.value?.role === 'child')
  const isParent = computed(() => user.value?.role === 'parent')

  function setAuth(userData, jwtToken) {
    user.value = userData
    token.value = jwtToken
    localStorage.setItem('ket_token', jwtToken)
    localStorage.setItem('ket_user', JSON.stringify(userData))
  }

  function restoreSession() {
    const saved = localStorage.getItem('ket_user')
    if (saved && token.value) {
      try {
        user.value = JSON.parse(saved)
      } catch {
        clearAuth()
      }
    }
  }

  async function login(username, password) {
    const data = await apiLogin(username, password)
    setAuth({
      userId: data.userId,
      username: data.username,
      role: data.role,
      nickname: data.nickname,
      streak: data.streak || 0,
      stars: data.stars || 0,
    }, data.token)
    return data
  }

  async function register(form) {
    const data = await apiRegister(form)
    setAuth({
      userId: data.userId,
      username: data.username,
      role: data.role,
      nickname: data.nickname,
      streak: data.streak || 0,
      stars: data.stars || 0,
    }, data.token)
    return data
  }

  async function fetchMe() {
    const data = await getMe()
    user.value = {
      userId: data.userId,
      username: data.username,
      role: data.role,
      nickname: data.nickname,
      streak: data.streak || 0,
      stars: data.stars || 0,
    }
    localStorage.setItem('ket_user', JSON.stringify(user.value))
    return user.value
  }

  function clearAuth() {
    user.value = null
    token.value = null
    localStorage.removeItem('ket_token')
    localStorage.removeItem('ket_user')
  }

  function logout() {
    clearAuth()
  }

  return {
    user,
    token,
    isLoggedIn,
    isChild,
    isParent,
    login,
    register,
    fetchMe,
    logout,
    clearAuth,
    setAuth,
    restoreSession,
  }
})
