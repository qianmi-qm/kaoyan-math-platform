import axios from 'axios'
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

// 统一剥壳：后端 Result{code,message,data} → 直接返回 data；非 0 或 HTTP 错误统一抛 Error
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body.code !== 0) {
      return Promise.reject(new Error(body.message || `错误码 ${body.code}`))
    }
    return body.data
  },
  (error) => {
    const body = error.response?.data
    const message = body?.message
      || (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请稍后重试')
    return Promise.reject(new Error(message))
  },
)

export default {  // L2 题库
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

  // L5 复习
  todayReviews: () =>
    http.get('/reviews/today').then((rows) => rows.map(normalizeReviewTask)),
  submitReview: (reviewId, remembered) =>
    http.post(`/reviews/${reviewId}/result`, null, { params: { remembered } })
      .then(normalizeReviewTask),
  reviewStats: () => http.get('/reviews/stats').then(normalizeReviewStats),
}
