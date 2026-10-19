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
    <div v-if="error" class="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm">
      {{ error }}
    </div>
    <!-- 顶部操作区 -->
    <section class="bg-white rounded-xl border border-slate-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold">题库管理</h2>
        <span class="text-sm text-slate-500">共 {{ total }} 题</span>
      </div>
      <div class="flex flex-wrap gap-3 items-center">
        <select
          v-model="selectedKp"
          @change="loadQuestions(true)"
          class="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/30"
        >
          <option value="">全部知识点</option>
          <optgroup v-for="parent in kpList.filter(k => k.level === 1)" :key="parent.id" :label="parent.name">
            <option
              v-for="child in kpList.filter(k => k.parentId === parent.id)"
              :key="child.id"
              :value="child.id"
            >{{ child.name }}</option>
          </optgroup>
        </select>

        <label v-if="isAdmin()" class="flex items-center gap-2 cursor-pointer">
          <input type="file" accept=".xlsx,.xls" @change="e => importFile = e.target.files[0]" class="hidden">
          <span class="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-sm transition">
            {{ importFile ? importFile.name : '选择 Excel' }}
          </span>
        </label>
        <button
          v-if="isAdmin()"
          @click="handleImport"
          :disabled="!importFile"
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white rounded-lg text-sm font-medium transition"
        >导入</button>
        <span v-else class="text-xs text-slate-400">只读题库 · 仅管理员可导入</span>
      </div>

      <!-- 导入结果 -->
      <div v-if="importResult" class="mt-4 p-4 bg-slate-50 rounded-lg border border-slate-200">
        <div class="flex gap-6 text-sm">
          <span>共 <strong>{{ importResult.total }}</strong> 行</span>
          <span class="text-emerald-600">成功 <strong>{{ importResult.success }}</strong></span>
          <span class="text-amber-600">重复 <strong>{{ importResult.duplicate }}</strong></span>
          <span class="text-rose-600">失败 <strong>{{ importResult.failed }}</strong></span>
        </div>
        <div v-if="importResult.failedRows?.length" class="mt-2 text-xs text-rose-600">
          <div v-for="(row, i) in importResult.failedRows" :key="i">
            第 {{ row.row }} 行：{{ row.reason }}
          </div>
        </div>
      </div>
    </section>

    <!-- 题目列表 -->
    <section class="space-y-3">
      <div v-if="loading" class="text-center py-12 text-slate-400">加载中...</div>
      <div v-else-if="!questions.length" class="text-center py-12 text-slate-400">暂无题目</div>
      <article
        v-for="q in questions"
        :key="q.id"
        class="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-300 hover:shadow-sm transition"
      >
        <div class="flex items-start justify-between gap-4 mb-3">
          <div class="flex items-center gap-2 flex-wrap">
            <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', diffColor(q.difficulty)]">
              {{ q.difficultyLabel }}
            </span>
            <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
              {{ q.typeLabel }}
            </span>
            <span v-for="name in q.kpNames" :key="name" class="text-xs text-slate-500">
              #{{ name }}
            </span>
          </div>
          <span class="text-xs text-slate-400">#{{ q.id }}</span>
        </div>
        <div class="text-slate-700 leading-relaxed" v-html="renderMath(q.content)"></div>
        <!-- 选项 -->
        <div v-if="q.options" class="mt-3 space-y-1.5">
          <div
            v-for="opt in q.options"
            :key="opt"
            class="text-sm text-slate-600 px-3 py-1.5"
            v-html="renderMath(opt)"
          ></div>
        </div>
      </article>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex items-center justify-center gap-2 pt-2">
        <button
          @click="prevPage"
          :disabled="page <= 1"
          class="px-3 py-1.5 text-sm border border-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-50"
        >上一页</button>
        <span class="text-sm text-slate-600">第 {{ page }} / {{ totalPages }} 页</span>
        <button
          @click="nextPage"
          :disabled="page >= totalPages"
          class="px-3 py-1.5 text-sm border border-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-50"
        >下一页</button>
      </div>
    </section>
  </div>
</template>
