<script setup>
import { ref, computed, onUnmounted, onMounted } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import api from '../api'
import { authHeaders, clearAuth, isMockMode } from '../auth'
import { renderMath } from '../utils/math'
import { diffColor } from '../utils/ui'

const route = useRoute()

// 阶段: setup | practicing | result
const phase = ref('setup')
const mode = ref('kp') // kp | random | wrong
const selectedKp = ref('')
const count = ref(5)
const kpList = ref([])

const questions = ref([])
const currentIdx = ref(0)
const answers = ref({}) // recordId -> answer
const results = ref({}) // recordId -> { correct, correctAnswer, analysis, durationSec }
const error = ref('')
const submitting = ref(false)

const timer = ref(0)
let timerHandle = null
const aiText = ref('')
const aiError = ref('')
const aiLoading = ref(false)
const aiQuota = ref(null)
let aiAbort = null

const currentQ = computed(() => questions.value[currentIdx.value] || null)
const progress = computed(() => ((currentIdx.value + 1) / questions.value.length) * 100)

const allSubmitted = computed(() => {
  return questions.value.every(q => results.value[q.recordId] && results.value[q.recordId].isCorrect !== null)
})

const correctCount = computed(() => {
  return Object.values(results.value).filter(r => r.correct).length
})

async function loadKps() {
  try {
    kpList.value = await api.listKnowledgePoints()
    const leaf = kpList.value.find(k => !(k.children && k.children.length)) || kpList.value[0]
    if (leaf) selectedKp.value = leaf.id
  } catch (e) {
    error.value = e.message
  }
}

async function startPractice(questionId = null) {
  error.value = ''
  submitting.value = true
  try {
    const payload = questionId
      ? { mode: 'QUESTION', questionId, count: 1 }
      : { mode: mode.value, count: count.value }
    if (!questionId && mode.value === 'kp') payload.kpId = selectedKp.value
    questions.value = await api.startSession(payload)
    currentIdx.value = 0
    answers.value = {}
    results.value = {}
    timer.value = 0
    resetAi()
    phase.value = 'practicing'
    startTimer()
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

function startTimer() {
  timerHandle = setInterval(() => { timer.value++ }, 1000)
}
function stopTimer() {
  if (timerHandle) { clearInterval(timerHandle); timerHandle = null }
}

function formatTime(s) {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}

function selectOption(optionKey) {
  answers.value[currentQ.value.recordId] = optionKey
}

async function submitCurrent() {
  const q = currentQ.value
  const answer = answers.value[q.recordId] || ''
  error.value = ''
  submitting.value = true
  try {
    const res = await api.submitAnswer(q.recordId, { answer })
    results.value[q.recordId] = res
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

async function selfEvaluate(recordId, correct) {
  error.value = ''
  submitting.value = true
  try {
    results.value[recordId] = await api.selfEvaluate(recordId, correct)
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  if (currentIdx.value < questions.value.length - 1) {
    currentIdx.value++
    resetAi()
  }
}
function prevQuestion() {
  if (currentIdx.value > 0) {
    currentIdx.value--
    resetAi()
  }
}

function resetAi() {
  aiText.value = ''
  aiError.value = ''
  aiLoading.value = false
  if (aiAbort) {
    aiAbort.abort()
    aiAbort = null
  }
}

function renderAiText(text) {
  return renderMath(text)
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}

async function loadQuota() {
  try {
    aiQuota.value = await api.aiQuota()
  } catch {
    aiQuota.value = null
  }
}

async function explainWithAi(recordId) {
  resetAi()
  aiLoading.value = true
  aiAbort = new AbortController()
  try {
    const response = await fetch(`/api/ai/explain/${recordId}`, {
      method: 'POST',
      headers: authHeaders(),
      signal: aiAbort.signal,
    })
    if (response.status === 401) {
      clearAuth()
      window.location.assign('/login')
      return
    }
    if (!response.ok || !response.body) {
      throw new Error(`讲解请求失败：${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let dataLines = []
    let eventName = ''

    const flushEvent = () => {
      const text = dataLines.join('\n')
      dataLines = []
      if (!text) {
        eventName = ''
        return false
      }
      if (eventName === 'error') {
        aiError.value = text
      } else if (text !== '[DONE]') {
        aiText.value += text
      }
      eventName = ''
      return text === '[DONE]'
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let newline = buffer.indexOf('\n')
      while (newline >= 0) {
        const line = buffer.slice(0, newline).replace(/\r$/, '')
        buffer = buffer.slice(newline + 1)
        if (line === '') {
          if (flushEvent()) return
        } else if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).replace(/^ /, ''))
        }
        newline = buffer.indexOf('\n')
      }
    }
    if (dataLines.length) flushEvent()
  } catch (e) {
    if (e.name !== 'AbortError') {
      aiError.value = e.message
    }
  } finally {
    aiLoading.value = false
    loadQuota()
  }
}

async function finishSession() {
  stopTimer()
  phase.value = 'result'
}

function reset() {
  phase.value = 'setup'
  stopTimer()
  timer.value = 0
  resetAi()
}

onMounted(async () => {
  await loadKps()
  loadQuota()
  const questionId = Number(route.query.questionId)
  if (Number.isFinite(questionId) && questionId > 0) {
    await startPractice(questionId)
  }
})

onBeforeRouteLeave(() => {
  if (phase.value !== 'practicing' || allSubmitted.value) return true
  return window.confirm('本组练习还没完成，确定离开？已提交的记录会保留。')
})
onUnmounted(() => {
  stopTimer()
  if (aiAbort) aiAbort.abort()
})
</script>

<template>
  <div class="space-y-6">
    <header v-if="phase === 'setup'">
      <div class="eyebrow">Practice</div>
      <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">开始练习</h1>
      <p class="mt-2 text-sm text-slate-500">选一个模式，给自己一组专注的题。</p>
    </header>

    <div v-if="error" class="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm">
      {{ error }}
    </div>
    <!-- 选题阶段 -->
    <section v-if="phase === 'setup'" class="app-card p-5 sm:p-7">
      <h2 class="mb-5 text-base font-semibold text-slate-800">练习设置</h2>

      <div class="space-y-5">
        <div>
          <label class="text-sm font-medium text-slate-700 block mb-2">练习模式</label>
          <div class="flex gap-3">
            <button
              v-for="m in [
                { v: 'kp', label: '按知识点', desc: '指定知识点抽题' },
                { v: 'random', label: '随机抽题', desc: '全题库随机' },
                { v: 'wrong', label: '错题重练', desc: '只练最近答错的题' },
              ]"
              :key="m.v"
              @click="mode = m.v"
              :class="[
                'flex-1 p-4 border-2 rounded-xl text-left transition',
                mode === m.v ? 'border-indigo-500 bg-indigo-50' : 'border-slate-200 hover:border-slate-300'
              ]"
            >
              <div class="font-medium text-slate-800">{{ m.label }}</div>
              <div class="text-xs text-slate-500 mt-1">{{ m.desc }}</div>
            </button>
          </div>
        </div>

        <div v-if="mode === 'kp'">
          <label class="text-sm font-medium text-slate-700 block mb-2">选择知识点</label>
          <select
            v-model="selectedKp"
            class="w-full md:w-80 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/30"
          >
            <option v-for="kp in kpList.filter(k => !(k.children && k.children.length))" :key="kp.id" :value="kp.id">
              {{ kp.name }}
            </option>
          </select>
        </div>

        <div>
          <label class="text-sm font-medium text-slate-700 block mb-2">题目数量：{{ count }} 题</label>
          <input
            type="range"
            v-model.number="count"
            min="3" max="15" step="1"
            class="w-full md:w-80 accent-indigo-600"
          >
        </div>

        <button
          @click="startPractice()"
          class="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium transition"
        >开始练习</button>
      </div>
    </section>

    <!-- 答题阶段 -->
    <section v-else-if="phase === 'practicing' && currentQ" class="space-y-4">
      <!-- 进度 + 计时 -->
      <div class="bg-white rounded-xl border border-slate-200 p-4 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="text-sm text-slate-600">
            第 <strong class="text-indigo-600">{{ currentIdx + 1 }}</strong> / {{ questions.length }} 题
          </div>
          <div class="w-48 h-2 bg-slate-100 rounded-full overflow-hidden">
            <div class="h-full bg-indigo-500 transition-all" :style="{ width: progress + '%' }"></div>
          </div>
        </div>
        <div class="text-lg font-mono text-slate-700 tabular-nums">
          ⏱ {{ formatTime(timer) }}
        </div>
      </div>

      <!-- 题目卡 -->
      <article class="bg-white rounded-xl border border-slate-200 p-6">
        <div class="flex items-center gap-2 mb-4 flex-wrap">
          <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', diffColor(currentQ.difficulty)]">
            {{ currentQ.difficultyLabel }}
          </span>
          <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
            {{ currentQ.typeLabel }}
          </span>
          <span v-for="name in currentQ.kpNames" :key="name" class="text-xs text-slate-500">
            #{{ name }}
          </span>
        </div>

        <div class="text-slate-800 text-lg leading-relaxed mb-6" v-html="renderMath(currentQ.content)"></div>

        <!-- 单选题 -->
        <div v-if="currentQ.type === 'SINGLE' && !results[currentQ.recordId]" class="space-y-2">
          <button
            v-for="opt in currentQ.options"
            :key="opt"
            @click="selectOption(opt.charAt(0))"
            :class="[
              'w-full text-left px-4 py-3 border rounded-lg transition',
              answers[currentQ.recordId] === opt.charAt(0)
                ? 'border-indigo-500 bg-indigo-50'
                : 'border-slate-200 hover:border-slate-300'
            ]"
            v-html="renderMath(opt)"
          ></button>
        </div>

        <!-- 填空题 -->
        <div v-else-if="currentQ.type === 'FILL' && !results[currentQ.recordId]" class="space-y-3">
          <label class="text-sm text-slate-600">你的答案（可输入 LaTeX）</label>
          <input
            v-model="answers[currentQ.recordId]"
            type="text"
            placeholder="如：\\frac{1}{2}"
            class="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/30 font-mono"
          >
        </div>

        <!-- 大题：先提交占位答案，再对照答案自评 -->
        <div v-else-if="currentQ.type === 'SUBJECTIVE' && !results[currentQ.recordId]" class="space-y-3">
          <label class="text-sm text-slate-600">你的解答（可输入 LaTeX）</label>
          <textarea
            v-model="answers[currentQ.recordId]"
            rows="5"
            placeholder="写出你的推导过程或最终答案"
            class="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/30 font-mono"
          ></textarea>
          <p class="text-xs text-slate-500">提交后对照答案自评，答错会自动进入复习队列。</p>
        </div>

        <!-- 结果展示 -->
        <div v-else class="mt-4 space-y-3">
          <div
            v-if="results[currentQ.recordId].isCorrect !== null"
            :class="[
              'p-4 rounded-lg border',
              results[currentQ.recordId].isCorrect
                ? 'bg-emerald-50 border-emerald-200'
                : 'bg-rose-50 border-rose-200'
            ]"
          >
            <div class="font-semibold mb-1" :class="results[currentQ.recordId].isCorrect ? 'text-emerald-700' : 'text-rose-700'">
              {{ results[currentQ.recordId].isCorrect ? '✓ 回答正确' : '✗ 回答错误' }}
            </div>
            <div class="text-sm text-slate-700">
              正确答案：<span class="font-mono" v-html="renderMath('$' + results[currentQ.recordId].correctAnswer + '$')"></span>
            </div>
            <div class="text-sm text-slate-600 mt-1">用时 {{ results[currentQ.recordId].durationSec }} 秒</div>
          </div>

          <div v-else class="p-4 rounded-lg border border-indigo-200 bg-indigo-50">
            <div class="font-semibold text-indigo-700 mb-1">已提交，请对照答案自评</div>
            <div class="flex gap-3 mt-3">
              <button
                @click="selfEvaluate(currentQ.recordId, false)"
                :disabled="submitting"
                class="flex-1 px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-sm font-medium disabled:opacity-40"
              >我答错了</button>
              <button
                @click="selfEvaluate(currentQ.recordId, true)"
                :disabled="submitting"
                class="flex-1 px-4 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-lg text-sm font-medium disabled:opacity-40"
              >我答对了</button>
            </div>
          </div>

          <div class="p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div class="text-sm font-medium text-slate-700 mb-1">解析</div>
            <div class="text-sm text-slate-600 leading-relaxed" v-html="renderMath(results[currentQ.recordId].analysis)"></div>
          </div>

          <div v-if="results[currentQ.recordId].isCorrect === false && !isMockMode()" class="flex items-center justify-end gap-3">
            <span v-if="aiQuota" class="mr-auto text-xs text-slate-400">
              今日 AI 剩余 {{ aiQuota.remaining }} 次
            </span>
            <button
              @click="explainWithAi(currentQ.recordId)"
              :disabled="aiLoading"
              class="px-4 py-2 bg-violet-50 hover:bg-violet-100 text-violet-700 border border-violet-200 rounded-lg text-sm font-medium transition disabled:opacity-40"
            >{{ aiLoading ? 'AI 讲解生成中…' : 'AI 讲解' }}</button>
          </div>

          <div v-if="aiLoading || aiText || aiError" class="p-4 bg-violet-50/60 rounded-lg border border-violet-200">
            <div class="text-sm font-medium text-violet-700 mb-2">AI 讲解</div>
            <div v-if="aiError" class="text-sm text-rose-600">{{ aiError }}</div>
            <div v-if="aiText" class="text-sm text-slate-700 leading-relaxed" v-html="renderAiText(aiText)"></div>
            <div v-else-if="aiLoading" class="text-sm text-slate-500">正在生成…</div>
          </div>
        </div>
      </article>

      <!-- 底部操作 -->
      <div class="flex items-center justify-between">
        <button
          @click="prevQuestion"
          :disabled="currentIdx === 0"
          class="px-4 py-2 border border-slate-300 rounded-lg text-sm disabled:opacity-40 hover:bg-slate-50"
        >上一题</button>

        <div class="flex gap-2">
          <button
            v-if="!results[currentQ.recordId]"
            @click="submitCurrent"
            :disabled="!answers[currentQ.recordId]"
            class="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white rounded-lg text-sm font-medium transition"
          >提交答案</button>
          <button
            v-else-if="currentIdx < questions.length - 1"
            @click="nextQuestion"
            class="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
          >下一题</button>
        </div>

        <button
          @click="finishSession"
          :disabled="!allSubmitted"
          class="px-4 py-2 border border-slate-300 rounded-lg text-sm disabled:opacity-40 hover:bg-slate-50"
        >结束练习</button>
      </div>
    </section>

    <!-- 结果阶段 -->
    <section v-else-if="phase === 'result'" class="space-y-6">
      <div class="bg-white rounded-xl border border-slate-200 p-6 text-center">
        <div class="text-sm text-slate-500 mb-2">本次练习成绩</div>
        <div class="text-5xl font-bold text-indigo-600 mb-4">
          {{ Math.round(correctCount / questions.length * 100) }}<span class="text-2xl">%</span>
        </div>
        <div class="flex justify-center gap-8 text-sm text-slate-600">
          <div>答对 <strong class="text-emerald-600">{{ correctCount }}</strong> 题</div>
          <div>答错 <strong class="text-rose-600">{{ questions.length - correctCount }}</strong> 题</div>
          <div>用时 <strong>{{ formatTime(timer) }}</strong></div>
        </div>
        <button
          @click="reset"
          class="mt-6 px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium transition"
        >再来一组</button>
      </div>

      <!-- 逐题回顾 -->
      <div class="space-y-3">
        <h3 class="font-medium text-slate-700">逐题回顾</h3>
        <article
          v-for="(q, i) in questions"
          :key="q.recordId"
          :class="[
            'bg-white rounded-xl border p-4',
            results[q.recordId]?.correct ? 'border-emerald-200' : 'border-rose-200'
          ]"
        >
          <div class="flex items-center gap-2 mb-2">
            <span class="text-sm font-medium text-slate-500">第 {{ i + 1 }} 题</span>
            <span
              :class="[
                'text-xs px-2 py-0.5 rounded-full font-medium',
                results[q.recordId]?.correct ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
              ]"
            >{{ results[q.recordId]?.correct ? '正确' : '错误' }}</span>
          </div>
          <div class="text-slate-700 text-sm" v-html="renderMath(q.content)"></div>
        </article>
      </div>
    </section>
  </div>
</template>
