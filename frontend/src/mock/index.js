// Mock 数据层：通过自定义 adapter 拦截请求，直接返回模拟数据
// 开启方式：URL 加 ?mock=1 或 localStorage.mock = '1'
import Mock from './data.js'

const MOCK_KEY = 'mock'

function isMockEnabled() {
  if (typeof localStorage === 'undefined') return false
  const url = new URLSearchParams(window.location.search).get(MOCK_KEY)
  if (url === '1') { localStorage.setItem(MOCK_KEY, '1'); return true }
  if (url === '0') { localStorage.removeItem(MOCK_KEY); return false }
  return localStorage.getItem(MOCK_KEY) === '1'
}

export function initMock(http) {
  if (!isMockEnabled()) return
  console.info('[mock] 已启用模拟数据，所有 /api 请求将返回本地 mock')

  http.defaults.adapter = (config) => {
    // config.url 是相对路径（如 /questions），去掉开头 / 用于匹配
    const data = mockResponse(config)
    if (data === undefined) {
      return Promise.reject({
        message: `[mock] 未匹配接口: ${config.method} ${config.url}`,
        config,
      })
    }
    return Promise.resolve({
      data: { code: 0, message: 'ok', data },
      status: 200,
      statusText: 'OK',
      headers: { 'content-type': 'application/json' },
      config,
      request: { mock: true },
    })
  }
}

function mockResponse(config) {
  // config.url 形如 /questions /practice/sessions 等（baseURL 不在 config.url 里）
  const url = (config.url || '').replace(/^\//, '')
  const method = (config.method || 'get').toLowerCase()
  const params = config.params || {}
  let body = {}
  try { body = config.data ? JSON.parse(config.data) : {} } catch {}

  // 题库
  if (url === 'knowledge-points' && method === 'get') return Mock.knowledgePoints
  if (url === 'questions' && method === 'get') return Mock.questionPage(params)
  if (url === 'questions/import' && method === 'post') return Mock.importResult()

  // 练习
  if (url === 'practice/sessions' && method === 'post') return Mock.startSession(body)
  if (url.match(/^practice\/records\/\d+\/submit$/) && method === 'post') {
    const id = Number(url.split('/')[2])
    return Mock.submitAnswer(id, body)
  }
  if (url === 'practice/records' && method === 'get') return Mock.practiceRecords(params.date)

  // 报告
  if (url === 'reports/daily' && method === 'get') return Mock.dailyReport(params.date)
  if (url === 'reports/daily/range' && method === 'get') return Mock.reportRange(params.from, params.to)

  // 复习
  if (url === 'reviews/today' && method === 'get') return Mock.todayReviews()
  if (url.match(/^reviews\/\d+\/result$/) && method === 'post') return { ok: true }
  if (url === 'reviews/stats' && method === 'get') return Mock.reviewStats()

  return undefined
}
