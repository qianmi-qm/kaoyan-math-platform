<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import { renderMath } from '../utils/math'

const tab = ref('all')
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const error = ref('')
const selected = ref(null)
const pendingId = ref(null)
const similar = ref([])
const similarLoading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function load(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size }
    if (tab.value === 'favorite') params.favorite = true
    const data = await api.photoHistory(params)
    records.value = data.list || []
    total.value = data.total || 0
    if (selected.value) {
      const fresh = records.value.find(item => item.id === selected.value.id)
      selected.value = fresh || null
    }
  } catch (e) {
    error.value = e.message
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchTab(next) {
  tab.value = next
  selected.value = null
  similar.value = []
  load(true)
}

async function openRecord(record) {
  if (selected.value?.id === record.id) {
    selected.value = null
    similar.value = []
    return
  }
  selected.value = record
  similar.value = []
  similarLoading.value = true
  try {
    similar.value = await api.photoSimilar(record.id, 4)
  } catch {
    similar.value = []
  } finally {
    similarLoading.value = false
  }
}

async function toggleFavorite(record) {
  if (pendingId.value) return
  pendingId.value = record.id
  error.value = ''
  try {
    const updated = await api.photoFavorite(record.id, !record.favorite)
    record.favorite = updated.favorite
    if (selected.value?.id === record.id) selected.value.favorite = updated.favorite
    if (tab.value === 'favorite' && !updated.favorite) await load()
  } catch (e) {
    error.value = e.message
  } finally {
    pendingId.value = null
  }
}

async function remove(record) {
  if (!window.confirm('删除这条搜题记录？原始图片本来就没有保存。')) return
  if (pendingId.value) return
  pendingId.value = record.id
  error.value = ''
  try {
    await api.photoHistoryDelete(record.id)
    if (selected.value?.id === record.id) selected.value = null
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    pendingId.value = null
  }
}

function prevPage() { if (page.value > 1) { page.value--; load() } }
function nextPage() { if (page.value < totalPages.value) { page.value++; load() } }

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <div class="eyebrow">Photo Archive</div>
        <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">搜题历史</h1>
        <p class="mt-2 text-sm text-slate-500">只保存识别后的题干与解析，不保存原始图片。</p>
      </div>
      <RouterLink to="/photo" class="btn-primary">再拍一题</RouterLink>
    </header>

    <div v-if="error" class="rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-600">{{ error }}</div>

    <div class="flex items-center gap-1 rounded-xl border border-slate-200/70 bg-white p-1 shadow-sm">
      <button
        class="rounded-lg px-4 py-2 text-sm font-medium transition"
        :class="tab === 'all' ? 'bg-slate-900 text-white' : 'text-slate-500 hover:bg-slate-50'"
        @click="switchTab('all')"
      >全部记录</button>
      <button
        class="rounded-lg px-4 py-2 text-sm font-medium transition"
        :class="tab === 'favorite' ? 'bg-slate-900 text-white' : 'text-slate-500 hover:bg-slate-50'"
        @click="switchTab('favorite')"
      >收藏 / 错题本</button>
      <span class="ml-auto pr-2 text-[11px] text-slate-400">{{ total }} 条</span>
    </div>

    <div v-if="loading" class="app-card flex items-center justify-center gap-2 py-16 text-sm text-slate-400">
      <span class="h-2 w-2 animate-pulse rounded-full bg-indigo-400"></span>加载中…
    </div>

    <div v-else-if="!records.length" class="app-card py-16 text-center">
      <div class="text-4xl opacity-30">◌</div>
      <div class="mt-3 text-sm font-medium text-slate-600">{{ tab === 'favorite' ? '还没有收藏的搜题记录' : '还没有搜题记录' }}</div>
      <div class="mt-1 text-xs text-slate-400">去拍一道题，记录会自动出现在这里</div>
      <RouterLink to="/photo" class="btn-secondary mt-5">去拍照搜题</RouterLink>
    </div>

    <div v-else class="space-y-3">
      <article
        v-for="record in records"
        :key="record.id"
        class="app-card overflow-hidden transition hover:border-indigo-200"
      >
        <div class="flex items-start gap-3 p-5">
          <button class="min-w-0 flex-1 text-left" @click="openRecord(record)">
            <div class="mb-2 flex flex-wrap items-center gap-2">
              <span class="chip bg-slate-100 text-slate-500">{{ formatDate(record.createdAt) }}</span>
              <span v-if="record.confidence" class="chip bg-emerald-50 text-emerald-600">置信度 {{ Math.round(record.confidence * 100) }}%</span>
              <span v-for="kp in record.knowledgePoints?.slice(0, 2)" :key="kp" class="chip bg-indigo-50 text-indigo-500">{{ kp }}</span>
            </div>
            <div class="line-clamp-2 text-sm leading-7 text-slate-700" v-html="renderMath(record.content)"></div>
            <div v-if="record.answer" class="mt-1 line-clamp-1 text-xs text-slate-400">
              答案：<span class="inline-block align-middle" v-html="renderMath(record.answer)"></span>
            </div>
            <div class="mt-2 text-[11px] text-indigo-500">{{ selected?.id === record.id ? '收起解析' : '查看解析 →' }}</div>
          </button>
          <div class="flex shrink-0 gap-1">
            <button
              class="rounded-lg px-2 py-1.5 text-lg transition hover:bg-amber-50"
              :class="record.favorite ? 'text-amber-400' : 'text-slate-300'"
              :disabled="pendingId === record.id"
              :title="record.favorite ? '取消收藏' : '收藏到错题本'"
              @click="toggleFavorite(record)"
            >★</button>
            <button class="rounded-lg px-2 py-1.5 text-xs text-slate-300 hover:bg-rose-50 hover:text-rose-500" @click="remove(record)">删除</button>
          </div>
        </div>

        <div v-if="selected?.id === record.id" class="border-t border-slate-100 bg-slate-50/50 p-5">
          <div class="space-y-4">
            <div class="text-[15px] leading-8 text-slate-800" v-html="renderMath(record.content)"></div>
            <div v-if="record.options?.length" class="grid gap-2 sm:grid-cols-2">
              <div v-for="(opt, i) in record.options" :key="i" class="rounded-xl border border-slate-100 bg-white px-3 py-2 text-sm text-slate-600" v-html="renderMath(opt)"></div>
            </div>
            <div v-if="record.answer" class="rounded-xl border border-indigo-100 bg-indigo-50 p-4">
              <div class="text-[11px] font-medium uppercase tracking-[0.16em] text-indigo-400">Answer</div>
              <div class="mt-1 text-slate-800" v-html="renderMath(record.answer)"></div>
            </div>
            <div v-if="record.steps?.length">
              <div class="mb-2 text-xs font-semibold text-slate-600">解析步骤</div>
              <ol class="space-y-2">
                <li v-for="(step, i) in record.steps" :key="i" class="flex gap-2.5 text-sm leading-7 text-slate-700">
                  <span class="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-white text-[10px] font-semibold text-indigo-500 ring-1 ring-indigo-100">{{ i + 1 }}</span>
                  <div v-html="renderMath(step)"></div>
                </li>
              </ol>
            </div>
            <div v-if="record.warnings?.length" class="rounded-xl border border-amber-100 bg-amber-50 p-3 text-xs text-amber-700">
              <div v-for="w in record.warnings" :key="w">⚠ {{ w }}</div>
            </div>

            <div class="border-t border-slate-200/70 pt-4">
              <div class="mb-3 flex items-center justify-between">
                <div class="text-xs font-semibold text-slate-600">练一道同类题</div>
                <span v-if="similarLoading" class="text-[11px] text-slate-400">匹配中…</span>
              </div>
              <div v-if="similar.length" class="grid gap-2 sm:grid-cols-2">
                <div v-for="q in similar" :key="q.id" class="flex flex-col justify-between gap-2 rounded-xl border border-slate-200 bg-white p-3">
                  <div class="line-clamp-2 text-xs leading-6 text-slate-600" v-html="renderMath(q.content)"></div>
                  <RouterLink
                    :to="{ path: '/practice', query: { questionId: q.id } }"
                    class="self-start rounded-lg bg-indigo-50 px-2.5 py-1 text-[11px] font-medium text-indigo-600 hover:bg-indigo-100"
                  >练这道 →</RouterLink>
                </div>
              </div>
              <div v-else-if="!similarLoading" class="text-xs text-slate-400">题库里还没有同类题。</div>
            </div>
          </div>
        </div>
      </article>

      <div v-if="total > size" class="flex items-center justify-center gap-3 pt-2">
        <button class="btn-secondary !px-3 !py-1.5" :disabled="page <= 1" @click="prevPage">上一页</button>
        <span class="text-xs text-slate-400">第 {{ page }} / {{ totalPages }} 页</span>
        <button class="btn-secondary !px-3 !py-1.5" :disabled="page >= totalPages" @click="nextPage">下一页</button>
      </div>
    </div>
  </div>
</template>
