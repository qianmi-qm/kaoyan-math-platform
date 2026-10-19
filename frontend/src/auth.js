import { reactive } from 'vue'

const TOKEN_KEY = 'kaoyan_token'
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
  user: readUser(),
})

export function setAuth(data) {
  authState.token = data.token
  authState.user = { id: data.userId, username: data.username }
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(USER_KEY, JSON.stringify(authState.user))
}

export function clearAuth() {
  authState.token = ''
  authState.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
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
