import axios from 'axios'
import { authState, clearAuth, setAuth, syncFromStorage } from '../auth'
import {
  flattenKnowledgePoints,
  normalizeImportResult,
  normalizePracticeRecord,
  normalizeQuestionPage,
  normalizeReport,
  normalizeReportRange,
  normalizeReviewStats,
  normalizeReviewTask,
} from './normalize'

export const http = axios.create({ baseURL: '/api', timeout: 15000 })

let refreshPromise = null

http.interceptors.request.use((config) => {
  if (authState.token) {
    config.headers.Authorization = `Bearer ${authState.token}`
  }
  return config
})

// 统一剥壳：后端 Result{code,message,data} → 直接返回 data；非 0 或 HTTP 错误统一抛 Error
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body.code !== 0) {
      return Promise.reject(new Error(body.message || `错误码 ${body.code}`))
    }
    return body.data
  },
  async (error) => {
    const body = error.response?.data
    const original = error.config
    const url = original?.url || ''
    const shouldRefresh = error.response?.status === 401
      && authState.refreshToken
      && !original?._retry
      && !url.includes('/auth/login')
      && !url.includes('/auth/register')
      && !url.includes('/auth/refresh')

    if (shouldRefresh) {
      const previousToken = authState.token
      original._retry = true
      try {
        if (!refreshPromise) {
          refreshPromise = axios.post('/api/auth/refresh', {
            refreshToken: authState.refreshToken,
          }).then((resp) => {
            if (resp.data.code !== 0) throw new Error(resp.data.message || '刷新失败')
            setAuth(resp.data.data)
            return resp.data.data.token
          }).finally(() => {
            refreshPromise = null
          })
        }
        const token = await refreshPromise
        original.headers = original.headers || {}
        original.headers.Authorization = `Bearer ${token}`
        return http(original)
      } catch {
        // 另一个标签页可能已经刷新成功；同步它的新 token 后重试一次
        syncFromStorage()
        if (authState.token && authState.token !== previousToken) {
          original.headers = original.headers || {}
          original.headers.Authorization = `Bearer ${authState.token}`
          try {
            return await http(original)
          } catch {
            // fall through to clear + login
          }
        }
      }
    }

    if (error.response?.status === 401) {
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    const message = body?.message
      || (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请稍后重试')
    return Promise.reject(new Error(message))
  },
)

export default {  // v3 认证
  login: (payload) => http.post('/auth/login', payload),
  register: (payload) => http.post('/auth/register', payload),
  me: () => http.get('/auth/me'),
  logout: () => http.post('/auth/logout'),
  changePassword: (payload) => http.post('/auth/password', payload),
  forgotPassword: (payload) => http.post('/auth/forgot-password', payload),
  resetPassword: (payload) => http.post('/auth/reset-password', payload),
  verifyEmail: (payload) => http.post('/auth/verify-email', payload),
  updateEmail: (payload) => http.post('/auth/email', payload),
  resendVerification: () => http.post('/auth/email/resend'),
  aiQuota: () => http.get('/ai/quota'),
  photoSearch: (file) => {
    const form = new FormData()
    form.append('file', file)
    return http.post('/ai/photo-search', form, { timeout: 120000 })
  },
  photoHistory: (params) => http.get('/ai/photo-history', { params }),
  savePhotoHistory: (payload) => http.post('/ai/photo-history', payload),
  photoHistoryDetail: (id) => http.get(`/ai/photo-history/${id}`),
  photoFavorite: (id, favorite) => http.patch(`/ai/photo-history/${id}/favorite`, { favorite }),
  photoHistoryDelete: (id) => http.delete(`/ai/photo-history/${id}`),
  photoSimilar: (id, limit = 4) => http.get(`/ai/photo-history/${id}/similar`, { params: { limit } }),
  adminUsers: () => http.get('/admin/users'),
  updateUserRole: (id, role) => http.patch(`/admin/users/${id}/role`, { role }),
  questionDetail: (id) => http.get(`/questions/${id}`),
  createQuestion: (payload) => http.post('/questions', payload),
  updateQuestion: (id, payload) => http.put(`/questions/${id}`, payload),

  // L2 题库
  listQuestions: (params) => http.get('/questions', { params }).then(normalizeQuestionPage),
  importExcel: (formData) =>
    http.post('/questions/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(normalizeImportResult),
  listKnowledgePoints: () => http.get('/knowledge-points').then(flattenKnowledgePoints),

  // L3 练习
  startSession: (payload) =>
    http.post('/practice/sessions', payload).then((rows) => rows.map(normalizePracticeRecord)),
  submitAnswer: (recordId, payload) =>
    http.post(`/practice/records/${recordId}/submit`, payload).then(normalizePracticeRecord),
  selfEvaluate: (recordId, correct) =>
    http.post(`/practice/records/${recordId}/self-eval`, { correct }).then(normalizePracticeRecord),
  listByDate: (date) =>
    http.get('/practice/records', { params: { date } }).then((rows) => rows.map(normalizePracticeRecord)),

  // L4 报告
  dailyReport: (date) =>
    http.get('/reports/daily', { params: { date } }).then(normalizeReport),
  reportRange: (from, to) =>
    http.get('/reports/daily/range', { params: { from, to } }).then(normalizeReportRange),
  learningOverview: (days = 30) =>
    http.get('/reports/overview', { params: { days } }),

  // L5 复习
  todayReviews: () =>
    http.get('/reviews/today').then((rows) => rows.map(normalizeReviewTask)),
  submitReview: (reviewId, remembered) =>
    http.post(`/reviews/${reviewId}/result`, null, { params: { remembered } })
      .then(normalizeReviewTask),
  reviewStats: () => http.get('/reviews/stats').then(normalizeReviewStats),
}
