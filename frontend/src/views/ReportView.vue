<script setup>
import { ref, onMounted, nextTick, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, RadarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import api from '../api'

echarts.use([LineChart, RadarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const reportDate = ref(new Date().toISOString().slice(0, 10))
const report = ref(null)
const trend = ref([])
const loading = ref(false)

let trendChart = null
let radarChart = null

async function loadReport() {
  loading.value = true
  try {
    report.value = await api.dailyReport(reportDate.value)
  } finally {
    loading.value = false
  }
}

async function loadTrend() {
  const today = new Date()
  const from = new Date(today)
  from.setDate(today.getDate() - 13)
  const data = await api.reportRange(
    from.toISOString().slice(0, 10),
    today.toISOString().slice(0, 10)
  )
  trend.value = data
}

function renderTrendChart() {
  const el = document.getElementById('trend-chart')
  if (!el || !trend.value.length) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(el)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['正确率', '平均掌握度'], bottom: 0 },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: trend.value.map(d => d.date.slice(5)),
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 1,
      axisLabel: { formatter: v => Math.round(v * 100) + '%' },
    },
    series: [
      {
        name: '正确率',
        type: 'line',
        smooth: true,
        data: trend.value.map(d => d.correctRate),
        itemStyle: { color: '#6366f1' },
        areaStyle: { opacity: 0.1 },
      },
      {
        name: '平均掌握度',
        type: 'line',
        smooth: true,
        data: trend.value.map(d => d.avgMastery),
        itemStyle: { color: '#10b981' },
        areaStyle: { opacity: 0.1 },
      },
    ],
  })
}

function renderRadarChart() {
  const el = document.getElementById('radar-chart')
  const list = report.value?.kpMasteryList
  if (!el || !list?.length) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(el)
  const topKps = list.slice(0, 6)
  radarChart.setOption({
    tooltip: {},
    radar: {
      indicator: topKps.map(k => ({ name: k.kpName, max: 1 })),
      radius: '65%',
      axisName: { fontSize: 12, color: '#64748b' },
    },
    series: [{
      type: 'radar',
      data: [{
        value: topKps.map(k => k.mastery),
        name: '掌握度',
        areaStyle: { opacity: 0.3 },
        itemStyle: { color: '#6366f1' },
        lineStyle: { color: '#6366f1' },
      }],
    }],
  })
}

function handleResize() {
  trendChart?.resize()
  radarChart?.resize()
}

function masteryLabel(v) {
  if (v >= 0.8) return { text: '掌握良好', color: 'text-emerald-600 bg-emerald-50' }
  if (v >= 0.6) return { text: '基本掌握', color: 'text-amber-600 bg-amber-50' }
  return { text: '需加强', color: 'text-rose-600 bg-rose-50' }
}

onMounted(async () => {
  await Promise.all([loadReport(), loadTrend()])
  await nextTick()
  renderTrendChart()
  renderRadarChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  radarChart?.dispose()
})

watch(reportDate, async () => {
  await loadReport()
  await nextTick()
  renderRadarChart()
})
</script>

<template>
  <div class="space-y-6">
    <!-- 顶部日期选择 + 概览卡片 -->
    <section class="bg-white rounded-xl border border-slate-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold">每日学习报告</h2>
        <input
          type="date"
          v-model="reportDate"
          class="px-3 py-1.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/30"
        >
      </div>

      <div v-if="loading" class="text-center py-8 text-slate-400">加载中...</div>
      <div v-else-if="report" class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="p-4 bg-indigo-50 rounded-xl">
          <div class="text-xs text-indigo-600 mb-1">练习题数</div>
          <div class="text-2xl font-bold text-indigo-700">{{ report.totalCount }}</div>
          <div class="text-xs text-indigo-500 mt-1">答对 {{ report.correctCount }} 题</div>
        </div>
        <div class="p-4 bg-emerald-50 rounded-xl">
          <div class="text-xs text-emerald-600 mb-1">正确率</div>
          <div class="text-2xl font-bold text-emerald-700">{{ Math.round(report.correctRate * 100) }}%</div>
          <div class="text-xs text-emerald-500 mt-1">较昨日 +2.3%</div>
        </div>
        <div class="p-4 bg-amber-50 rounded-xl">
          <div class="text-xs text-amber-600 mb-1">平均用时</div>
          <div class="text-2xl font-bold text-amber-700">{{ report.avgDurationSec }}<span class="text-sm">秒</span></div>
          <div class="text-xs text-amber-500 mt-1">每题平均</div>
        </div>
        <div class="p-4 bg-violet-50 rounded-xl">
          <div class="text-xs text-violet-600 mb-1">综合掌握度</div>
          <div class="text-2xl font-bold text-violet-700">{{ Math.round(report.avgMastery * 100) }}%</div>
          <div class="text-xs text-violet-500 mt-1">正确率×速度×连对</div>
        </div>
      </div>
    </section>

    <!-- 图表区 -->
    <div class="grid md:grid-cols-3 gap-6">
      <section class="bg-white rounded-xl border border-slate-200 p-5 md:col-span-2">
        <h3 class="font-medium text-slate-700 mb-3">近 14 天趋势</h3>
        <div id="trend-chart" class="h-64 w-full"></div>
      </section>

      <section class="bg-white rounded-xl border border-slate-200 p-5">
        <h3 class="font-medium text-slate-700 mb-3">知识点掌握雷达</h3>
        <div id="radar-chart" class="h-64 w-full"></div>
      </section>
    </div>

    <!-- 薄弱知识点 -->
    <section class="bg-white rounded-xl border border-slate-200 p-5">
      <h3 class="font-medium text-slate-700 mb-4">薄弱知识点（需重点复习）</h3>
      <div v-if="report?.weakPoints?.length" class="space-y-3">
        <div
          v-for="wp in report.weakPoints"
          :key="wp.kpId"
          class="flex items-center justify-between p-3 bg-rose-50 border border-rose-100 rounded-lg"
        >
          <div>
            <div class="font-medium text-rose-800">{{ wp.kpName }}</div>
            <div class="text-xs text-rose-600">已练习 {{ wp.questionCount }} 题</div>
          </div>
          <div class="text-right">
            <div class="text-lg font-bold text-rose-700">{{ Math.round(wp.mastery * 100) }}%</div>
            <div class="text-xs text-rose-500">掌握度</div>
          </div>
        </div>
      </div>
      <div v-else class="text-center py-8 text-slate-400 text-sm">暂无薄弱点，继续保持！</div>
    </section>

    <!-- 各知识点掌握度列表 -->
    <section class="bg-white rounded-xl border border-slate-200 p-5">
      <h3 class="font-medium text-slate-700 mb-4">知识点掌握度一览</h3>
      <div class="space-y-3">
        <div
          v-for="kp in report?.kpMasteryList || []"
          :key="kp.kpId"
          class="flex items-center gap-4"
        >
          <div class="w-28 text-sm text-slate-700 truncate">{{ kp.kpName }}</div>
          <div class="flex-1 h-2.5 bg-slate-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="kp.mastery >= 0.7 ? 'bg-emerald-500' : kp.mastery >= 0.5 ? 'bg-amber-500' : 'bg-rose-500'"
              :style="{ width: (kp.mastery * 100) + '%' }"
            ></div>
          </div>
          <div class="w-12 text-right text-sm font-medium text-slate-700 tabular-nums">
            {{ Math.round(kp.mastery * 100) }}%
          </div>
          <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', masteryLabel(kp.mastery).color]">
            {{ masteryLabel(kp.mastery).text }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>
