<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../api'
import { renderMath } from '../utils/math'

const todayList = ref([])
const stats = ref(null)
const loading = ref(false)
const error = ref('')
const finishedIds = ref(new Set())
const pendingIds = ref(new Set())

const remaining = computed(() => todayList.value.filter(t => !finishedIds.value.has(t.id)))
const completed = computed(() => todayList.value.filter(t => finishedIds.value.has(t.id)))

const INTERVALS = [1, 2, 4, 7, 15]
const STAGE_COLORS = ['bg-indigo-500', 'bg-violet-500', 'bg-sky-500', 'bg-amber-400', 'bg-emerald-500']

const stageTotal = computed(() => (stats.value?.stageCounts || []).reduce((sum, item) => sum + item.count, 0))

function stagePercent(item) {
  if (!stageTotal.value) return 0
  return (item.count / stageTotal.value) * 100
}

async function loadReviews() {
  loading.value = true
  error.value = ''
  try {
    todayList.value = await api.todayReviews()
  } catch (e) {
    error.value = e.message
    todayList.value = []
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await api.reviewStats()
  } catch (e) {
    error.value = e.message
  }
}

async function handleRemember(item, remembered) {
  if (pendingIds.value.has(item.id)) return
  pendingIds.value.add(item.id)
  error.value = ''
  try {
    await api.submitReview(item.id, remembered)
    finishedIds.value.add(item.id)
    await loadStats()
  } catch (e) {
    error.value = e.message
  } finally {
    pendingIds.value.delete(item.id)
  }
}

function stageLabel(stage) {
  return `第 ${stage + 1} 轮 / 共 5 轮`
}

function nextIntervalDays(stage) {
  return INTERVALS[Math.min(stage, INTERVALS.length - 1)]
}

onMounted(() => { loadReviews(); loadStats() })
</script>

<template>
  <div class="space-y-6">
    <header>
      <div class="eyebrow">Spaced Repetition</div>
      <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">今日复习</h1>
      <p class="mt-2 text-sm text-slate-500">按艾宾浩斯 5 轮周期，把今天该复习的题过一遍。</p>
    </header>

    <div v-if="error" class="rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-600">
      {{ error }}
    </div>

    <section class="app-card p-5 sm:p-6">
      <div class="mb-5 flex flex-wrap items-center justify-between gap-2">
        <h2 class="text-base font-semibold text-slate-800">复习概览</h2>
        <span class="chip bg-slate-100 text-slate-500">5 轮周期 · 间隔 1 / 2 / 4 / 7 / 15 天</span>
      </div>

      <div v-if="stats" class="mb-6 grid grid-cols-2 gap-3 md:grid-cols-4">
        <div class="rounded-2xl bg-indigo-50/80 p-4">
          <div class="text-[11px] font-medium text-indigo-500">今日到期</div>
          <div class="mt-1 text-2xl font-bold text-indigo-700">{{ stats.dueToday }}</div>
          <div class="mt-1 text-[11px] text-indigo-400">待复习</div>
        </div>
        <div class="rounded-2xl bg-amber-50/80 p-4">
          <div class="text-[11px] font-medium text-amber-600">队列总数</div>
          <div class="mt-1 text-2xl font-bold text-amber-700">{{ stats.activeTotal }}</div>
          <div class="mt-1 text-[11px] text-amber-500">进行中</div>
        </div>
        <div class="rounded-2xl bg-emerald-50/80 p-4">
          <div class="text-[11px] font-medium text-emerald-600">已完成</div>
          <div class="mt-1 text-2xl font-bold text-emerald-700">{{ stats.completedTotal }}</div>
          <div class="mt-1 text-[11px] text-emerald-500">全部 5 轮</div>
        </div>
        <div class="rounded-2xl bg-violet-50/80 p-4">
          <div class="text-[11px] font-medium text-violet-600">今日已复习</div>
          <div class="mt-1 text-2xl font-bold text-violet-700">{{ finishedIds.size }}</div>
          <div class="mt-1 text-[11px] text-violet-500">继续加油</div>
        </div>
      </div>

      <div v-if="stats?.stageCounts" class="border-t border-slate-100 pt-4">
        <div class="mb-3 flex items-center justify-between">
          <div class="text-xs font-medium text-slate-600">各周期积压分布</div>
          <div class="text-[11px] text-slate-400">共 {{ stageTotal }} 题</div>
        </div>
        <div class="flex h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            v-for="(sc, i) in stats.stageCounts"
            :key="sc.stage"
            :class="STAGE_COLORS[i]"
            :style="{ width: stagePercent(sc) + '%' }"
            :title="`第 ${sc.stage + 1} 轮：${sc.count} 题`"
          ></div>
        </div>
        <div class="mt-3 grid grid-cols-5 gap-1 text-center">
          <div v-for="(sc, i) in stats.stageCounts" :key="sc.stage">
            <div class="text-[11px] font-medium text-slate-700">{{ sc.count }}</div>
            <div class="text-[10px] text-slate-400">{{ INTERVALS[sc.stage] }}天</div>
          </div>
        </div>
      </div>
    </section>

    <section>
      <h3 class="mb-3 flex items-center justify-between text-sm font-semibold text-slate-700">
        <span>待复习 <span class="font-normal text-slate-400">（{{ remaining.length }} 题）</span></span>
        <span v-if="completed.length" class="text-xs font-normal text-emerald-600">已完成 {{ completed.length }} 题</span>
      </h3>

      <div v-if="loading" class="app-card flex items-center justify-center gap-2 py-14 text-sm text-slate-400">
        <span class="h-2 w-2 animate-pulse rounded-full bg-indigo-400"></span>加载中…
      </div>

      <div v-else-if="!remaining.length" class="app-card py-14 text-center">
        <div class="text-4xl">🎉</div>
        <div class="mt-3 text-sm font-medium text-slate-700">今日复习全部完成</div>
        <div class="mt-1 text-xs text-slate-400">去题库再练几题，或者明天见</div>
      </div>

      <div v-else class="space-y-3">
        <article
          v-for="item in remaining"
          :key="item.id"
          class="app-card p-5 transition"
          :class="item.overdue ? '!border-rose-200 bg-rose-50/30' : ''"
        >
          <div class="mb-3 flex flex-wrap items-start justify-between gap-3">
            <div class="flex flex-wrap items-center gap-2">
              <span class="chip bg-indigo-50 text-indigo-600">{{ stageLabel(item.stage) }}</span>
              <span v-if="item.overdue" class="chip bg-rose-50 text-rose-600">⚠ 逾期补做</span>
            </div>
            <span class="text-[11px] text-slate-400">下次间隔：{{ nextIntervalDays(item.stage) }} 天</span>
          </div>

          <div class="mb-4 text-[15px] leading-8 text-slate-700" v-html="renderMath(item.questionPreview)"></div>

          <div class="flex gap-2.5">
            <button
              @click="handleRemember(item, false)"
              :disabled="pendingIds.has(item.id)"
              class="flex-1 rounded-xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-sm font-medium text-rose-700 transition hover:bg-rose-100 disabled:opacity-40"
            >忘了 · 重置</button>
            <button
              @click="handleRemember(item, true)"
              :disabled="pendingIds.has(item.id)"
              class="flex-1 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2.5 text-sm font-medium text-emerald-700 transition hover:bg-emerald-100 disabled:opacity-40"
            >记住了 · 推进</button>
          </div>
        </article>
      </div>
    </section>

    <section class="app-card p-5">
      <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700"><span>📖</span>复习规则</h4>
      <ul class="list-inside list-disc space-y-2 text-xs leading-6 text-slate-500">
        <li><strong class="text-slate-700">5 轮周期</strong>：第 1、2、4、7、15 天各复习一次，完成后题目从队列移除。</li>
        <li><strong class="text-slate-700">记住了</strong>：进入下一周期，下次复习日 = 今天 + 下一周期天数。</li>
        <li><strong class="text-slate-700">忘了</strong>：重置回第 1 轮，明天重新开始。</li>
        <li><strong class="text-slate-700">逾期补做</strong>：错过的日子不会自动跳级，从你实际复习日重新计算。</li>
        <li><strong class="text-slate-700">核心公式</strong>：掌握度 = 0.5×正确率 + 0.3×速度 + 0.2×连对，低于 0.6 自动入队。</li>
      </ul>
    </section>
  </div>
</template>
