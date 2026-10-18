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
    <div v-if="error" class="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm">
      {{ error }}
    </div>
    <!-- 顶部统计 -->
    <section class="bg-white rounded-xl border border-slate-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold">今日复习</h2>
        <span class="text-sm text-slate-500">艾宾浩斯遗忘曲线 · 5 轮周期</span>
      </div>

      <div v-if="stats" class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="p-4 bg-indigo-50 rounded-xl text-center">
          <div class="text-xs text-indigo-600 mb-1">今日到期</div>
          <div class="text-2xl font-bold text-indigo-700">{{ stats.dueToday }}</div>
          <div class="text-xs text-indigo-500 mt-1">待复习</div>
        </div>
        <div class="p-4 bg-amber-50 rounded-xl text-center">
          <div class="text-xs text-amber-600 mb-1">队列总数</div>
          <div class="text-2xl font-bold text-amber-700">{{ stats.activeTotal }}</div>
          <div class="text-xs text-amber-500 mt-1">进行中</div>
        </div>
        <div class="p-4 bg-emerald-50 rounded-xl text-center">
          <div class="text-xs text-emerald-600 mb-1">已完成</div>
          <div class="text-2xl font-bold text-emerald-700">{{ stats.completedTotal }}</div>
          <div class="text-xs text-emerald-500 mt-1">全部 5 轮</div>
        </div>
        <div class="p-4 bg-violet-50 rounded-xl text-center">
          <div class="text-xs text-violet-600 mb-1">今日已复习</div>
          <div class="text-2xl font-bold text-violet-700">{{ finishedIds.size }}</div>
          <div class="text-xs text-violet-500 mt-1">继续加油</div>
        </div>
      </div>

      <!-- 各周期积压 -->
      <div v-if="stats?.stageCounts" class="pt-4 border-t border-slate-100">
        <div class="text-sm text-slate-600 mb-3">各周期积压分布</div>
        <div class="flex items-end gap-3 h-24">
          <div
            v-for="sc in stats.stageCounts"
            :key="sc.stage"
            class="flex-1 flex flex-col items-center gap-1"
          >
            <div
              class="w-full bg-indigo-500 rounded-t transition-all"
              :style="{ height: Math.max(4, sc.count / Math.max(...stats.stageCounts.map(s => s.count)) * 64) + 'px' }"
            ></div>
            <div class="text-xs text-slate-500">{{ INTERVALS[sc.stage] }}天</div>
            <div class="text-xs font-medium text-slate-700">{{ sc.count }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 待复习清单 -->
    <section>
      <h3 class="font-medium text-slate-700 mb-3 flex items-center justify-between">
        <span>待复习（{{ remaining.length }} 题）</span>
        <span v-if="completed.length" class="text-sm font-normal text-emerald-600">已完成 {{ completed.length }} 题</span>
      </h3>

      <div v-if="loading" class="text-center py-12 text-slate-400 bg-white rounded-xl border border-slate-200">加载中...</div>

      <div v-else-if="!remaining.length" class="text-center py-12 text-slate-400 bg-white rounded-xl border border-slate-200">
        <div class="text-4xl mb-2">🎉</div>
        <div>今日复习全部完成！</div>
      </div>

      <div v-else class="space-y-3">
        <article
          v-for="item in remaining"
          :key="item.id"
          :class="[
            'bg-white rounded-xl border p-5 transition',
            item.overdue ? 'border-rose-300 bg-rose-50/30' : 'border-slate-200'
          ]"
        >
          <div class="flex items-start justify-between gap-4 mb-3 flex-wrap">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="text-xs px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700 font-medium">
                {{ stageLabel(item.stage) }}
              </span>
              <span v-if="item.overdue" class="text-xs px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 font-medium">
                ⚠ 逾期补做
              </span>
            </div>
            <span class="text-xs text-slate-400">下次间隔：{{ nextIntervalDays(item.stage) }} 天</span>
          </div>

          <div class="text-slate-700 leading-relaxed mb-4" v-html="renderMath(item.questionPreview)"></div>

          <div class="flex gap-3">
            <button
              @click="handleRemember(item, false)"
              :disabled="pendingIds.has(item.id)"
              class="flex-1 px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-sm font-medium transition disabled:opacity-40"
            >忘了 · 重置</button>
            <button
              @click="handleRemember(item, true)"
              :disabled="pendingIds.has(item.id)"
              class="flex-1 px-4 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-lg text-sm font-medium transition disabled:opacity-40"
            >记住了 · 推进</button>
          </div>
        </article>
      </div>
    </section>

    <!-- 复习规则说明 -->
    <section class="bg-slate-50 border border-slate-200 rounded-xl p-5">
      <h4 class="font-medium text-slate-700 mb-3">📖 复习规则</h4>
      <ul class="text-sm text-slate-600 space-y-2 list-disc list-inside">
        <li><strong>5 轮周期</strong>：第 1、2、4、7、15 天各复习一次，完成后题目从队列移除。</li>
        <li><strong>记住了</strong>：进入下一周期，下次复习日 = 今天 + 下一周期天数。</li>
        <li><strong>忘了</strong>：重置回第 1 轮，明天重新开始。</li>
        <li><strong>逾期补做</strong>：错过的日子不会自动跳级，从你实际复习日重新计算。</li>
        <li><strong>核心公式</strong>：掌握度 = 0.5×正确率 + 0.3×速度 + 0.2×连对，低于 0.6 自动入队。</li>
      </ul>
    </section>
  </div>
</template>
