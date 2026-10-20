// 把后端 VO 适配成页面使用的统一结构。
// 后端契约变化只需要改这一层，页面不再各自猜字段。
const TYPE_NAME = { 1: 'SINGLE', 2: 'FILL', 3: 'SUBJECTIVE' }
const TYPE_LABEL = { 1: '单选', 2: '填空', 3: '大题' }
const DIFFICULTY_LABEL = { 1: '易', 2: '中', 3: '难' }

export function parseOptions(raw) {
  if (Array.isArray(raw)) return raw
  if (!raw) return null
  try {
    const value = JSON.parse(raw)
    return Array.isArray(value) ? value : null
  } catch {
    return null
  }
}

function questionLike(raw) {
  const type = typeof raw.type === 'number' ? (TYPE_NAME[raw.type] || String(raw.type)) : raw.type
  return {
    ...raw,
    type,
    typeLabel: TYPE_LABEL[raw.type] || raw.typeLabel || '',
    difficultyLabel: DIFFICULTY_LABEL[raw.difficulty] || raw.difficultyLabel || '',
    options: parseOptions(raw.options),
  }
}

export function normalizeQuestionPage(data) {
  return {
    ...data,
    records: (data?.list || data?.records || []).map(questionLike),
  }
}

export function flattenKnowledgePoints(tree) {
  if (!Array.isArray(tree)) return []
  // mock 数据本身就是扁平结构（带 level），直接复用
  if (tree.some((node) => typeof node.level === 'number')) return tree
  const output = []
  const walk = (nodes, level) => {
    for (const node of nodes || []) {
      output.push({ ...node, level, parentId: node.parentId ?? null })
      walk(node.children, level + 1)
    }
  }
  walk(tree, 1)
  return output
}

export function normalizePracticeRecord(raw) {
  const rawCorrect = raw.isCorrect ?? raw.correct
  const correct = rawCorrect == null ? null : rawCorrect === true
  return {
    ...questionLike(raw),
    correct,
    isCorrect: correct,
  }
}

export function normalizeReport(raw) {
  const source = raw.kpMastery || raw.kpMasteryList || []
  const kpMastery = source.map((item) => ({ ...item, kpName: item.name || item.kpName }))
  const weakPoints = (raw.weakPoints || []).map((item) => ({
    ...item,
    kpName: item.name || item.kpName,
  }))
  const computedAverage = kpMastery.length
    ? kpMastery.reduce((sum, item) => sum + item.mastery, 0) / kpMastery.length
    : 0
  return {
    ...raw,
    date: raw.reportDate || raw.date,
    kpMastery,
    kpMasteryList: kpMastery,
    weakPoints,
    avgMastery: raw.overallMastery > 0 ? raw.overallMastery : computedAverage,
  }
}

export function normalizeReportRange(list) {
  return (list || []).map(normalizeReport)
}

export function normalizeReviewTask(raw) {
  return {
    ...questionLike(raw),
    id: raw.reviewId ?? raw.id,
    questionPreview: raw.content ?? raw.questionPreview,
  }
}

export function normalizeReviewStats(raw) {
  return {
    ...raw,
    stageCounts: raw.byStage || raw.stageCounts || [],
    completedTotal: raw.completedTotal ?? 0,
  }
}

export function normalizeImportResult(raw) {
  if (raw.total !== undefined && raw.totalRows === undefined) {
    return raw
  }
  return {
    total: raw.totalRows,
    success: raw.imported,
    duplicate: raw.duplicated,
    failed: raw.failed,
    failedRows: (raw.failures || []).map((item) => ({
      row: item.rowIndex,
      reason: item.reason,
    })),
  }
}
