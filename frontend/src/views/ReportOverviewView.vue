<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'

const days = ref(30)
const data = ref(null)
const loading = ref(false)
const error = ref('')

const durationText = computed(() => {
  const sec = data.value?.totalDurationSec || 0
  if (sec < 60) return `${sec} 秒`
  const minutes = Math.floor(sec / 60)
  return minutes < 60 ? `${minutes} 分钟` : `${(minutes / 60).toFixed(1)} 小时`
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await api.learningOverview(days.value)
  } catch (e) {
    error.value = e.message
    data.value = null
  } finally {
    loading.value = false
  }
}

function switchDays(next) {
  days.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-5">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <div class="eyebrow">Learning Analytics</div>
        <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">整体学习分析</h1>
        <p class="mt-2 text-sm text-slate-500">按时间窗口聚合，不与“今日报告”混在一起。</p>
      </div>
      <RouterLink to="/report" class="btn-secondary !px-3 !py-1.5 text-xs">返回每日报告</RouterLink>
    </header>

    <div class="flex items-center gap-1 rounded-xl border border-slate-200/70 bg-white p-1 shadow-sm">
      <button
        v-for="d in [7, 30, 90]"
        :key="d"
        class="flex-1 rounded-lg px-3 py-2 text-sm font-medium transition"
        :class="days === d ? 'bg-slate-900 text-white' : 'text-slate-500 hover:bg-slate-50'"
        @click="switchDays(d)"
      >近 {{ d }} 天</button>
    </div>

    <div v-if="error" class="rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-600">{{ error }}</div>

    <div v-if="loading" class="app-card flex items-center justify-center gap-2 py-16 text-sm text-slate-400">
      <span class="h-2 w-2 animate-pulse rounded-full bg-indigo-400"></span>分析中…
    </div>

    <template v-else-if="data">
      <section v-if="data.suggestions?.length" class="app-card p-5">
        <div class="mb-3 flex items-center justify-between">
          <h2 class="text-sm font-semibold text-slate-800">下一步建议</h2>
          <span class="chip bg-indigo-50 text-indigo-500">{{ data.days }} 天窗口</span>
        </div>
        <div class="grid gap-2 sm:grid-cols-2">
          <RouterLink
            v-for="s in data.suggestions"
            :key="s.title"
            :to="s.path"
            class="group rounded-xl border border-slate-100 bg-slate-50/70 p-3.5 transition hover:border-indigo-200 hover:bg-indigo-50/40"
          >
            <div class="text-sm font-semibold text-slate-800 group-hover:text-indigo-600">{{ s.title }}</div>
            <div class="mt-1 text-xs leading-5 text-slate-500">{{ s.description }}</div>
          </RouterLink>
        </div>
      </section>

      <section class="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div class="app-card p-4">
          <div class="text-[11px] text-slate-400">开始 / 已提交</div>
          <div class="mt-1 text-2xl font-bold text-slate-800">{{ data.startedCount }}<span class="text-sm text-slate-400"> / {{ data.submittedCount }}</span></div>
          <div class="mt-1 text-[11px]" :class="data.pendingCount ? 'text-amber-600' : 'text-slate-400'">待提交 {{ data.pendingCount }}</div>
        </div>
        <div class="app-card p-4">
          <div class="text-[11px] text-slate-400">正确率</div>
          <div class="mt-1 text-2xl font-bold text-emerald-600">{{ Math.round(data.correctRate * 100) }}%</div>
          <div class="mt-1 text-[11px] text-slate-400">有效判分 {{ data.gradedCount }} 题</div>
        </div>
        <div class="app-card p-4">
          <div class="text-[11px] text-slate-400">总学习时长</div>
          <div class="mt-1 text-2xl font-bold text-amber-600">{{ durationText }}</div>
          <div class="mt-1 text-[11px] text-slate-400">平均 {{ data.avgDurationSec }} 秒/题</div>
        </div>
        <div class="app-card p-4">
          <div class="text-[11px] text-slate-400">综合掌握度</div>
          <div class="mt-1 text-2xl font-bold text-indigo-600">{{ Math.round(data.overallMastery * 100) }}%</div>
          <div class="mt-1 text-[11px] text-slate-400">按题量加权</div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2">
        <div class="app-card p-5">
          <h2 class="mb-4 text-sm font-semibold text-slate-800">分题型</h2>
          <div v-if="data.byType?.length" class="space-y-3">
            <div v-for="row in data.byType" :key="row.key">
              <div class="mb-1 flex items-center justify-between text-xs">
                <span class="text-slate-600">{{ row.label }} <span class="text-slate-400">（{{ row.count }} 题）</span></span>
                <span class="font-medium text-slate-700">{{ Math.round(row.correctRate * 100) }}%</span>
              </div>
              <div class="h-2 overflow-hidden rounded-full bg-slate-100">
                <div class="h-full rounded-full bg-indigo-500" :style="{ width: (row.correctRate * 100) + '%' }"></div>
              </div>
              <div class="mt-1 text-[10px] text-slate-400">已判分 {{ row.gradedCount }} · 平均 {{ row.avgDurationSec }} 秒</div>
            </div>
          </div>
          <div v-else class="py-6 text-center text-xs text-slate-400">还没有提交数据</div>
        </div>

        <div class="app-card p-5">
          <h2 class="mb-4 text-sm font-semibold text-slate-800">分难度</h2>
          <div v-if="data.byDifficulty?.length" class="space-y-3">
            <div v-for="row in data.byDifficulty" :key="row.key">
              <div class="mb-1 flex items-center justify-between text-xs">
                <span class="text-slate-600">{{ row.label }} <span class="text-slate-400">（{{ row.count }} 题）</span></span>
                <span class="font-medium text-slate-700">{{ Math.round(row.correctRate * 100) }}%</span>
              </div>
              <div class="h-2 overflow-hidden rounded-full bg-slate-100">
                <div class="h-full rounded-full bg-sky-500" :style="{ width: (row.correctRate * 100) + '%' }"></div>
              </div>
              <div class="mt-1 text-[10px] text-slate-400">已判分 {{ row.gradedCount }} · 平均 {{ row.avgDurationSec }} 秒</div>
            </div>
          </div>
          <div v-else class="py-6 text-center text-xs text-slate-400">还没有提交数据</div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-3">
        <div class="app-card p-5 md:col-span-2">
          <div class="mb-4 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-slate-800">薄弱知识点</h2>
            <RouterLink to="/practice" class="text-xs text-indigo-600">去练习</RouterLink>
          </div>
          <div v-if="data.weakPoints?.length" class="space-y-2">
            <div v-for="kp in data.weakPoints" :key="kp.kpId" class="flex items-center gap-3 rounded-xl border border-rose-100 bg-rose-50/60 p-3">
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium text-rose-800">{{ kp.name || kp.kpName }}</div>
                <div class="text-[11px] text-rose-500">{{ kp.questionCount }} 题 · 需加强</div>
              </div>
              <div class="text-sm font-bold text-rose-700">{{ Math.round(kp.mastery * 100) }}%</div>
              <RouterLink :to="{ path: '/practice', query: { kpId: kp.kpId } }" class="rounded-lg bg-white px-2.5 py-1.5 text-[11px] text-rose-600 ring-1 ring-rose-200">去练</RouterLink>
            </div>
          </div>
          <div v-else class="py-8 text-center text-xs text-slate-400">这个窗口内没有达到阈值的薄弱点</div>
        </div>

        <div class="app-card p-5">
          <h2 class="mb-4 text-sm font-semibold text-slate-800">复习执行</h2>
          <div v-if="data.reviewStats" class="grid grid-cols-3 gap-2 text-center">
            <div class="rounded-xl bg-amber-50 p-3">
              <div class="text-lg font-bold text-amber-700">{{ data.reviewStats.dueToday }}</div>
              <div class="text-[10px] text-amber-500">今日到期</div>
            </div>
            <div class="rounded-xl bg-indigo-50 p-3">
              <div class="text-lg font-bold text-indigo-700">{{ data.reviewStats.activeTotal }}</div>
              <div class="text-[10px] text-indigo-500">队列中</div>
            </div>
            <div class="rounded-xl bg-emerald-50 p-3">
              <div class="text-lg font-bold text-emerald-700">{{ data.reviewStats.completedTotal }}</div>
              <div class="text-[10px] text-emerald-500">已完成</div>
            </div>
          </div>
          <RouterLink to="/review" class="btn-secondary mt-3 w-full !py-2 text-xs">去复习</RouterLink>
        </div>
      </section>

      <section class="app-card p-5">
        <h2 class="mb-4 text-sm font-semibold text-slate-800">知识点掌握度</h2>
        <div v-if="data.kpMastery?.length" class="space-y-3">
          <div v-for="kp in data.kpMastery" :key="kp.kpId" class="flex items-center gap-3">
            <div class="w-24 truncate text-xs text-slate-600 sm:w-32">{{ kp.name || kp.kpName }}</div>
            <div class="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
              <div class="h-full rounded-full" :class="kp.mastery >= 0.7 ? 'bg-emerald-500' : kp.mastery >= 0.5 ? 'bg-amber-500' : 'bg-rose-500'" :style="{ width: (kp.mastery * 100) + '%' }"></div>
            </div>
            <div class="w-10 text-right text-xs font-medium text-slate-700">{{ Math.round(kp.mastery * 100) }}%</div>
          </div>
        </div>
        <div v-else class="py-6 text-center text-xs text-slate-400">还没有可分析的知识点</div>
      </section>
    </template>
  </div>
</template>
