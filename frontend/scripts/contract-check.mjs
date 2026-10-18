import {
  flattenKnowledgePoints,
  normalizePracticeRecord,
  normalizeQuestionPage,
  normalizeReport,
  normalizeReviewStats,
  normalizeReviewTask,
} from '../src/api/normalize.js'

const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api'

function check(condition, message) {
  if (!condition) throw new Error(message)
}

async function get(path) {
  const response = await fetch(BASE + path)
  const body = await response.json()
  check(response.ok, `${path} HTTP ${response.status}`)
  check(body.code === 0, `${path} code ${body.code}`)
  return body.data
}

const tree = await get('/knowledge-points')
const flat = flattenKnowledgePoints(tree)
check(flat.length >= 6, 'knowledge points missing')
check(flat.some(k => k.level === 1), 'no level-1 knowledge point')
check(flat.some(k => k.level === 2), 'no level-2 knowledge point')

const page = normalizeQuestionPage(await get('/questions?page=1&size=10'))
check(page.records.length > 0, 'no questions')
check(typeof page.records[0].type === 'string', 'question type not normalized')
check(typeof page.records[0].difficultyLabel === 'string', 'difficulty label missing')
check(page.records[0].options === null || Array.isArray(page.records[0].options), 'options not array')

const report = normalizeReport(await get('/reports/daily?date=2026-10-18'))
check(typeof report.date === 'string', 'report date missing')
check(Array.isArray(report.kpMasteryList), 'kpMasteryList missing')
check(report.kpMasteryList.every(k => typeof k.kpName === 'string'), 'kpName missing')
check(typeof report.avgMastery === 'number', 'avgMastery missing')

const stats = normalizeReviewStats(await get('/reviews/stats'))
check(Array.isArray(stats.stageCounts), 'stageCounts missing')
check(typeof stats.completedTotal === 'number', 'completedTotal missing')

const synthetic = normalizeReviewTask({
  reviewId: 9, questionId: 1, content: '题', type: 1, options: '["A. 1"]', stage: 0,
})
check(synthetic.id === 9, 'review id missing')
check(synthetic.questionPreview === '题', 'questionPreview missing')
check(Array.isArray(synthetic.options), 'review options not parsed')

const practice = normalizePracticeRecord({
  recordId: 1, questionId: 1, content: '题', type: 3, options: null,
  isCorrect: false, durationSec: 2,
})
check(practice.correct === false, 'correct flag missing')
check(practice.type === 'SUBJECTIVE', 'type not normalized')

const pending = normalizePracticeRecord({
  recordId: 2, questionId: 3, content: '题', type: 3, options: null,
  isCorrect: null, durationSec: 2,
})
check(pending.isCorrect === null, 'pending self-eval must stay null')

console.log('CONTRACT_OK')
