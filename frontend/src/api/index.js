import axios from 'axios'

export const http = axios.create({ baseURL: '/api', timeout: 15000 })

// 统一剥壳：后端 Result{code,message,data} → 直接返回 data；非 0 抛错
http.interceptors.response.use((resp) => {
  const body = resp.data
  if (body.code !== 0) {
    return Promise.reject(new Error(body.message || `错误码 ${body.code}`))
  }
  return body.data
})

export default {
  // L2 题库
  listQuestions: (params) => http.get('/questions', { params }),
  importExcel: (formData) =>
    http.post('/questions/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
  listKnowledgePoints: () => http.get('/knowledge-points'),

  // L3 练习
  startSession: (payload) => http.post('/practice/sessions', payload),
  submitAnswer: (recordId, payload) => http.post(`/practice/records/${recordId}/submit`, payload),
  listByDate: (date) => http.get('/practice/records', { params: { date } }),

  // L4 报告
  dailyReport: (date) => http.get('/reports/daily', { params: { date } }),
  reportRange: (from, to) => http.get('/reports/daily/range', { params: { from, to } }),

  // L5 复习
  todayReviews: () => http.get('/reviews/today'),
  submitReview: (reviewId, remembered) =>
    http.post(`/reviews/${reviewId}/result`, null, { params: { remembered } }),
  reviewStats: () => http.get('/reviews/stats')
}
