import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserInfo, type UserInfo } from '../../api/student/auth'
import router from '../../router'

function getCachedUserInfo(): UserInfo | null {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) as UserInfo : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(getCachedUserInfo())

  async function login(username: string, password: string, remember = false) {
    const res = await loginApi({ username, password, remember })
    const data = res.data as { token: string }
    token.value = data.token

    // 始终写入 localStorage 做持久化（保证刷新不丢）
    // 也写入 sessionStorage 保证 router 守卫能读到
    localStorage.setItem('token', data.token)
    sessionStorage.setItem('token', data.token)

    if (remember) {
      // remember= true → localStorage 已写入，再加标记用于后续延长过期
      localStorage.setItem('remember', '1')
    }

    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data as UserInfo
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    sessionStorage.removeItem('token')
    localStorage.removeItem('remember')
    localStorage.removeItem('userInfo')
    router.push('/login')
  }

  return { token, userInfo, login, fetchUserInfo, logout }
})
