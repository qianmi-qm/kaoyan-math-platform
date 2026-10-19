<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { isAdmin } from '../auth'
import { renderMath } from '../utils/math'
import { diffColor } from '../utils/ui'

const kpList = ref([])
const selectedKp = ref('')
const page = ref(1)
const pageSize = 6
const total = ref(0)
const questions = ref([])
const loading = ref(false)
const importFile = ref(null)
const importResult = ref(null)
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadKps() {
  try {
    kpList.value = await api.listKnowledgePoints()
  } catch (e) {
    error.value = e.message
  }
}

async function loadQuestions(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  error.value = ''
  try {
    const data = await api.listQuestions({
      kpId: selectedKp.value || undefined,
      page: page.value,
      size: pageSize,
    })
    total.value = data.total
    questions.value = data.records
  } catch (e) {
    error.value = e.message
    questions.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function prevPage() { if (page.value > 1) { page.value--; loadQuestions() } }
function nextPage() { if (page.value < totalPages.value) { page.value++; loadQuestions() } }

async function handleImport() {
  if (!importFile.value) return
  error.value = ''
  const form = new FormData()
  form.append('file', importFile.value)
  try {
    importResult.value = await api.importExcel(form)
    await loadQuestions(true)
  } catch (e) {
    error.value = e.message
  }
}

onMounted(() => { loadKps(); loadQuestions() })
</script>

<template>
  <div class="space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <div class="eyebrow">Question Bank</div>
        <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">题库</h1>
        <p class="mt-2 text-sm text-slate-500">按知识点筛选，开始一组练习；错题会自动进入复习队列。</p>
      </div>
      <div class="flex items-center gap-2">
        <span class="chip bg-white text-slate-500 ring-1 ring-slate-200">共 {{ total }} 题</span>
        <RouterLink to="/practice" class="btn-primary">开始练习</RouterLink>
      </div>
    </header>

    <div v-if="error" class="flex items-start gap-2 rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-600">
      <span class="mt-0.5">!</span><span>{{ error }}</span>
    </div>

    <section class="app-card p-4 sm:p-5">
      <div class="flex flex-wrap items-center gap-3">
        <div class="relative">
          <select
            v-model="selectedKp"
            @change="loadQuestions(true)"
            class="input-field min-w-48 appearance-none pr-9"
          >
            <option value="">全部知识点</option>
            <option v-for="kp in kpList" :key="kp.id" :value="kp.id">
              {{ kp.level === 1 ? kp.name : '　' + kp.name }}
            </option>
          </select>
          <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400">⌄</span>
        </div>

        <template v-if="isAdmin()">
          <label class="btn-secondary cursor-pointer">
            <input type="file" accept=".xlsx,.xls" @change="e => importFile = e.target.files[0]" class="hidden">
            {{ importFile ? importFile.name : '批量导入 Excel' }}
          </label>
          <button
            @click="handleImport"
            :disabled="!importFile"
            class="btn-primary disabled:bg-slate-300"
          >导入</button>
        </template>
        <span v-else class="chip bg-slate-50 text-slate-400">只读题库 · 仅管理员可导入</span>
      </div>

      <div v-if="importResult" class="mt-4 rounded-xl border border-slate-100 bg-slate-50/80 p-4">
        <div class="flex flex-wrap gap-x-6 gap-y-1 text-xs text-slate-500">
          <span>共 <strong class="text-slate-700">{{ importResult.total }}</strong> 行</span>
          <span class="text-emerald-600">成功 <strong>{{ importResult.success }}</strong></span>
          <span class="text-amber-600">重复 <strong>{{ importResult.duplicate }}</strong></span>
          <span class="text-rose-600">失败 <strong>{{ importResult.failed }}</strong></span>
        </div>
        <div v-if="importResult.failedRows?.length" class="mt-2 space-y-1 text-xs text-rose-600">
          <div v-for="(row, i) in importResult.failedRows" :key="i">第 {{ row.row }} 行：{{ row.reason }}</div>
        </div>
      </div>
    </section>

    <section class="space-y-3">
      <div v-if="loading" class="app-card flex items-center justify-center gap-2 py-16 text-sm text-slate-400">
        <span class="h-2 w-2 animate-pulse rounded-full bg-indigo-400"></span>加载中…
      </div>
      <div v-else-if="!questions.length" class="app-card py-16 text-center">
        <div class="text-3xl opacity-30">∅</div>
        <p class="mt-2 text-sm text-slate-400">这个筛选下还没有题目</p>
        <RouterLink v-if="isAdmin()" to="/admin/questions" class="mt-3 inline-block text-xs text-indigo-600 hover:underline">去题库编辑新增</RouterLink>
      </div>

      <article
        v-for="q in questions"
        :key="q.id"
        class="app-card app-card-hover p-5 sm:p-6"
      >
        <div class="mb-4 flex items-start justify-between gap-4">
          <div class="flex flex-wrap items-center gap-2">
            <span :class="['chip', diffColor(q.difficulty)]">{{ q.difficultyLabel }}</span>
            <span class="chip bg-slate-100 text-slate-600">{{ q.typeLabel }}</span>
            <span v-for="name in q.kpNames" :key="name" class="text-[11px] text-slate-400">#{{ name }}</span>
          </div>
          <div class="flex items-center gap-3">
            <RouterLink
              :to="{ path: '/practice', query: { questionId: q.id } }"
              class="rounded-lg bg-indigo-50 px-3 py-1.5 text-xs font-medium text-indigo-600 transition hover:bg-indigo-100"
            >练这道</RouterLink>
            <span class="font-mono text-[11px] text-slate-300">#{{ q.id }}</span>
          </div>
        </div>
        <div class="text-[15px] leading-8 text-slate-800" v-html="renderMath(q.content)"></div>
        <div v-if="q.options" class="mt-4 grid gap-2 sm:grid-cols-2">
          <div
            v-for="opt in q.options"
            :key="opt"
            class="rounded-xl border border-slate-100 bg-slate-50/70 px-3.5 py-2 text-sm text-slate-600"
            v-html="renderMath(opt)"
          ></div>
        </div>
      </article>

      <div v-if="total > 0" class="flex items-center justify-center gap-3 pt-2 text-sm">
        <button @click="prevPage" :disabled="page <= 1" class="btn-secondary !px-3 !py-1.5">上一页</button>
        <span class="text-xs text-slate-400">第 {{ page }} / {{ totalPages }} 页</span>
        <button @click="nextPage" :disabled="page >= totalPages" class="btn-secondary !px-3 !py-1.5">下一页</button>
      </div>
    </section>
  </div>
</template>
