import { reactive } from 'vue'

const TOKEN_KEY = 'kaoyan_token'
const REFRESH_KEY = 'kaoyan_refresh_token'
const USER_KEY = 'kaoyan_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const authState = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  refreshToken: localStorage.getItem(REFRESH_KEY) || '',
  user: readUser(),
})

export function setAuth(data) {
  authState.token = data.token
  authState.refreshToken = data.refreshToken || ''
  authState.user = { id: data.userId, username: data.username, role: data.role || 'USER' }
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(REFRESH_KEY, authState.refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(authState.user))
}

export function clearAuth() {
  authState.token = ''
  authState.refreshToken = ''
  authState.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

export function updateUser(user) {
  if (!user) return
  authState.user = { ...authState.user, ...user }
  localStorage.setItem(USER_KEY, JSON.stringify(authState.user))
}

export function isMockMode() {
  return typeof localStorage !== 'undefined' && localStorage.getItem('mock') === '1'
}

export function isLoggedIn() {
  return Boolean(authState.token) || isMockMode()
}

export function authHeaders() {
  return authState.token ? { Authorization: `Bearer ${authState.token}` } : {}
}

export function isAdmin() {
  return authState.user?.role === 'ADMIN'
}
