// Mock 数据：模拟后端所有接口的返回值，包含真实数学公式、日期序列
// 数据结构与后端 Result.data 完全一致，前端代码无需区分真假

const knowledgePoints = [
  { id: 1, name: '函数与极限', parentId: 0, level: 1, sort: 1 },
  { id: 2, name: '数列极限', parentId: 1, level: 2, sort: 1 },
  { id: 3, name: '函数极限', parentId: 1, level: 2, sort: 2 },
  { id: 4, name: '无穷小与无穷大', parentId: 1, level: 2, sort: 3 },
  { id: 5, name: '导数与微分', parentId: 0, level: 1, sort: 2 },
  { id: 6, name: '导数概念', parentId: 5, level: 2, sort: 1 },
  { id: 7, name: '求导法则', parentId: 5, level: 2, sort: 2 },
  { id: 8, name: '微分中值定理', parentId: 5, level: 2, sort: 3 },
  { id: 9, name: '积分学', parentId: 0, level: 1, sort: 3 },
  { id: 10, name: '不定积分', parentId: 9, level: 2, sort: 1 },
  { id: 11, name: '定积分', parentId: 9, level: 2, sort: 2 },
  { id: 12, name: '反常积分', parentId: 9, level: 2, sort: 3 },
]

const questionBank = [
  {
    id: 1,
    content: '计算极限：$$\\lim_{x \\to 0} \\frac{\\sin x}{x}$$',
    type: 'SINGLE',
    difficulty: 1,
    options: ['A. 0', 'B. 1', 'C. $\\infty$', 'D. 不存在'],
    answer: 'B',
    analysis: '这是两个重要极限之一，结果为 1。可以用夹逼定理或洛必达法则证明。',
    kpIds: [3],
  },
  {
    id: 2,
    content: '设 $f(x) = x^2 \\sin\\frac{1}{x}$，求 $f\'(0)$。',
    type: 'FILL',
    difficulty: 2,
    answer: '0',
    analysis: '按导数定义：$f\'(0)=\\lim_{h\\to0}\\frac{h^2\\sin(1/h)-0}{h}=\\lim_{h\\to0}h\\sin(1/h)=0$，无穷小乘有界量。',
    kpIds: [6],
  },
  {
    id: 3,
    content: '计算不定积分：$\\int \\frac{1}{x^2 + a^2} dx$（$a > 0$）',
    type: 'FILL',
    difficulty: 2,
    answer: '\\frac{1}{a}\\arctan\\frac{x}{a}+C',
    analysis: '令 $x = a\\tan t$，则 $dx = a\\sec^2 t \\, dt$，积分化为 $\\frac{1}{a}\\int dt = \\frac{1}{a}\\arctan\\frac{x}{a}+C$。',
    kpIds: [10],
  },
  {
    id: 4,
    content: '下列函数在 $x=0$ 处可导的是：',
    type: 'SINGLE',
    difficulty: 2,
    options: ['A. $f(x) = |x|$', 'B. $f(x) = x|x|$', 'C. $f(x) = \\lfloor x \\rfloor$', 'D. $f(x) = \\sin\\frac{1}{x}$'],
    answer: 'B',
    analysis: 'A 左右导数不等(-1 和 1)；B 分段求导均为 0；C 不连续；D 在 0 处无定义。',
    kpIds: [6],
  },
  {
    id: 5,
    content: '计算定积分：$\\int_0^{\\pi} \\sin^2 x \\, dx$',
    type: 'FILL',
    difficulty: 2,
    answer: '\\frac{\\pi}{2}',
    analysis: '用降幂公式 $\\sin^2 x = \\frac{1-\\cos 2x}{2}$，积分得 $\\left[\\frac{x}{2}-\\frac{\\sin 2x}{4}\\right]_0^\\pi = \\frac{\\pi}{2}$。',
    kpIds: [11],
  },
  {
    id: 6,
    content: '求极限：$\\lim_{n \\to \\infty} \\left(\\frac{1}{n^2+1} + \\frac{1}{n^2+2} + \\cdots + \\frac{1}{n^2+n}\\right)$',
    type: 'FILL',
    difficulty: 3,
    answer: '0',
    analysis: '用夹逼定理：$\\frac{n}{n^2+n} \\le \\sum \\le \\frac{n}{n^2+1}$，两边极限均为 0。',
    kpIds: [2],
  },
  {
    id: 7,
    content: '若 $f(x)$ 在 $[a,b]$ 上连续，在 $(a,b)$ 内可导，则至少存在一点 $\\xi \\in (a,b)$ 使得：',
    type: 'SINGLE',
    difficulty: 2,
    options: [
      'A. $f(b)-f(a) = f\'(\\xi)(b-a)$',
      'B. $f(b)-f(a) = f\'(\\xi)$',
      'C. $f\'(\\xi) = 0$',
      'D. $f(\\xi) = 0$',
    ],
    answer: 'A',
    analysis: '这是拉格朗日中值定理的标准表述。选项 C 是罗尔定理（额外条件 f(a)=f(b)）。',
    kpIds: [8],
  },
  {
    id: 8,
    content: '反常积分 $\\int_1^{+\\infty} \\frac{1}{x^p} dx$ 收敛的充要条件是：',
    type: 'SINGLE',
    difficulty: 2,
    options: ['A. $p > 1$', 'B. $p \\ge 1$', 'C. $p < 1$', 'D. $p = 1$'],
    answer: 'A',
    analysis: 'p=1 时积分 $\\ln x$ 发散；p>1 时收敛到 $\\frac{1}{p-1}$；p<1 时发散。这是 p-积分的经典结论。',
    kpIds: [12],
  },
  {
    id: 9,
    content: '当 $x \\to 0$ 时，$\\alpha(x) = 1 - \\cos x$ 是 $\\beta(x) = x^2$ 的：',
    type: 'SINGLE',
    difficulty: 1,
    options: ['A. 高阶无穷小', 'B. 低阶无穷小', 'C. 同阶但非等价无穷小', 'D. 等价无穷小'],
    answer: 'C',
    analysis: '$\\lim_{x\\to0}\\frac{1-\\cos x}{x^2}=\\lim\\frac{x^2/2}{x^2}=\\frac{1}{2}$，比值为非零常数，所以同阶非等价。',
    kpIds: [4],
  },
  {
    id: 10,
    content: '求 $y = x^x$（$x > 0$）的导数。',
    type: 'FILL',
    difficulty: 3,
    answer: 'x^x(1+\\ln x)',
    analysis: '取对数：$\\ln y = x\\ln x$，两边求导得 $\\frac{y\'}{y} = \\ln x + 1$，故 $y\' = x^x(1+\\ln x)$。',
    kpIds: [7],
  },
  {
    id: 11,
    content: '设数列 $a_n = \\frac{n!}{n^n}$，则 $\\lim_{n\\to\\infty} a_n = $',
    type: 'FILL',
    difficulty: 3,
    answer: '0',
    analysis: '$\\frac{a_{n+1}}{a_n} = \\frac{(n+1)!}{(n+1)^{n+1}} \\cdot \\frac{n^n}{n!} = \\left(\\frac{n}{n+1}\\right)^n = \\frac{1}{(1+1/n)^n} \\to \\frac{1}{e} < 1$，由比值判别法级数收敛，通项趋于0。',
    kpIds: [2],
  },
  {
    id: 12,
    content: '函数 $f(x) = \\frac{x^2-1}{x-1}$ 在 $x=1$ 处：',
    type: 'SINGLE',
    difficulty: 1,
    options: ['A. 连续', 'B. 可导', 'C. 跳跃间断', 'D. 可去间断'],
    answer: 'D',
    analysis: '$f(x) = x+1$（$x\\neq1$），$\\lim_{x\\to1}f(x)=2$ 存在但不等于 f(1)（f(1) 无定义），故为可去间断点。',
    kpIds: [3],
  },
]

function pickKpName(cpdId) {
  const kp = knowledgePoints.find(k => k.id === cpdId)
  return kp ? kp.name : '未知'
}

function typeLabel(type) {
  return type === 'SINGLE' ? '单选' : '填空'
}

function difficultyLabel(difficulty) {
  return ['', '易', '中', '难'][difficulty]
}

function paginate(list, page, size) {
  const total = list.length
  const start = (page - 1) * size
  const records = list.slice(start, start + size)
  return { total, page, size, records }
}

function questionToVO(q) {
  return {
    id: q.id,
    content: q.content,
    type: q.type,
    typeLabel: typeLabel(q.type),
    difficulty: q.difficulty,
    difficultyLabel: difficultyLabel(q.difficulty),
    options: q.options || null,
    answer: q.answer,
    analysis: q.analysis,
    kpNames: q.kpIds.map(pickKpName),
  }
}

// 生成最近 14 天的报告趋势数据
function genTrend() {
  const days = []
  const today = new Date()
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const date = d.toISOString().slice(0, 10)
    const base = 0.55 + (13 - i) * 0.025 + (Math.sin(i) * 0.04)
    days.push({
      date,
      totalCount: 15 + Math.floor(Math.random() * 20),
      correctRate: Math.min(0.95, Math.max(0.4, base)),
      avgMastery: Math.min(0.9, Math.max(0.35, base - 0.05)),
      avgDurationSec: 90 + Math.floor(Math.random() * 40),
    })
  }
  return days
}

const trend = genTrend()

export default {
  knowledgePoints,

  questionPage(params = {}) {
    const page = params.page || 1
    const size = params.size || 8
    let list = questionBank
    if (params.kpId) {
      const kpId = Number(params.kpId)
      list = list.filter(q => q.kpIds.includes(kpId))
    }
    const result = paginate(list, page, size)
    result.records = result.records.map(questionToVO)
    return result
  },

  importResult() {
    return {
      total: 25,
      success: 22,
      duplicate: 2,
      failed: 1,
      failedRows: [{ row: 17, reason: '知识点"傅里叶级数"不存在' }],
    }
  },

  startSession(body) {
    const count = body.count || 5
    const shuffled = [...questionBank].sort(() => Math.random() - 0.5).slice(0, count)
    return shuffled.map((q, i) => ({
      recordId: 1000 + i,
      questionId: q.id,
      content: q.content,
      type: q.type,
      typeLabel: typeLabel(q.type),
      difficulty: q.difficulty,
      difficultyLabel: difficultyLabel(q.difficulty),
      options: q.options || null,
      kpNames: q.kpIds.map(pickKpName),
      startedAt: new Date().toISOString(),
    }))
  },

  submitAnswer(recordId, body) {
    // mock 判分：有 70% 概率答对
    const correct = Math.random() < 0.7
    const q = questionBank.find(q => q.id === (recordId % 1000)) || questionBank[0]
    return {
      recordId,
      correct,
      correctAnswer: q.answer,
      analysis: q.analysis,
      durationSec: 30 + Math.floor(Math.random() * 90),
      masteryDelta: correct ? 0.08 : -0.05,
    }
  },

  practiceRecords(dateStr) {
    return Array.from({ length: 6 }, (_, i) => ({
      id: 200 + i,
      questionId: i + 1,
      content: questionBank[i].content.slice(0, 30) + '...',
      type: questionBank[i].type,
      difficulty: questionBank[i].difficulty,
      isCorrect: i % 3 !== 2,
      durationSec: 40 + i * 10,
      submittedAt: `${dateStr || '2026-08-17'} ${10 + i}:${15 + i * 3}:00`,
    }))
  },

  dailyReport(dateStr) {
    const today = trend[trend.length - 1]
    return {
      date: dateStr || today.date,
      totalCount: today.totalCount,
      correctCount: Math.round(today.totalCount * today.correctRate),
      correctRate: today.correctRate,
      avgDurationSec: today.avgDurationSec,
      avgMastery: today.avgMastery,
      weakPoints: [
        { kpId: 2, kpName: '数列极限', mastery: 0.42, questionCount: 5 },
        { kpId: 12, kpName: '反常积分', mastery: 0.48, questionCount: 4 },
        { kpId: 8, kpName: '微分中值定理', mastery: 0.55, questionCount: 6 },
      ],
      kpMasteryList: knowledgePoints
        .filter(k => k.level === 2)
        .slice(0, 8)
        .map((k, i) => ({
          kpId: k.id,
          kpName: k.name,
          mastery: Math.min(0.95, 0.5 + i * 0.05 + Math.random() * 0.1),
          questionCount: 3 + i,
        })),
    }
  },

  reportRange() {
    return trend
  },

  todayReviews() {
    const due = [
      { id: 301, questionId: 1, questionPreview: '计算极限：$\\lim_{x \\to 0} \\frac{\\sin x}{x}$', stage: 2, nextReviewDate: todayStr(), overdue: false },
      { id: 302, questionId: 6, questionPreview: '求极限：$\\lim_{n \\to \\infty} \\left(\\frac{1}{n^2+1} + \\cdots + \\frac{1}{n^2+n}\\right)$', stage: 0, nextReviewDate: todayStr(), overdue: false },
      { id: 303, questionId: 10, questionPreview: '求 $y = x^x$（$x > 0$）的导数。', stage: 1, nextReviewDate: yesterdayStr(), overdue: true },
      { id: 304, questionId: 3, questionPreview: '计算不定积分：$\\int \\frac{1}{x^2 + a^2} dx$', stage: 3, nextReviewDate: todayStr(), overdue: false },
      { id: 305, questionId: 8, questionPreview: '反常积分 $\\int_1^{+\\infty} \\frac{1}{x^p} dx$ 收敛的充要条件', stage: 0, nextReviewDate: yesterdayStr(), overdue: true },
    ]
    return due
  },

  reviewStats() {
    return {
      dueToday: 5,
      activeTotal: 28,
      completedTotal: 14,
      stageCounts: [
        { stage: 0, count: 10 },
        { stage: 1, count: 7 },
        { stage: 2, count: 5 },
        { stage: 3, count: 4 },
        { stage: 4, count: 2 },
      ],
    }
  },
}

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}
function yesterdayStr() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return d.toISOString().slice(0, 10)
}
