<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../api'
import { renderMath } from '../utils/math'

const fileInput = ref(null)
const sourceFile = ref(null)
const preview = ref('')
const result = ref(null)
const error = ref('')
const loading = ref(false)
const quota = ref(null)

async function loadQuota() {
  try {
    quota.value = await api.aiQuota()
  } catch {
    quota.value = null
  }
}

function chooseFile() {
  fileInput.value?.click()
}

function onFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  error.value = ''
  result.value = null
  sourceFile.value = file
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = URL.createObjectURL(file)
}

async function compressImage(file) {
  if (!file.type.startsWith('image/')) throw new Error('请选择图片文件')
  const bitmap = await createImageBitmap(file)
  const maxSide = 1600
  const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(bitmap.width * scale))
  canvas.height = Math.max(1, Math.round(bitmap.height * scale))
  const ctx = canvas.getContext('2d')
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  bitmap.close?.()
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(new File([blob], 'question.jpg', { type: 'image/jpeg' }))
      else reject(new Error('图片压缩失败'))
    }, 'image/jpeg', 0.86)
  })
}

async function search() {
  error.value = ''
  if (!sourceFile.value) {
    error.value = '请先拍照或选择题目图片'
    return
  }
  loading.value = true
  result.value = null
  try {
    const compressed = await compressImage(sourceFile.value)
    result.value = await api.photoSearch(compressed)
    await loadQuota()
  } catch (e) {
    error.value = e.message || '搜题失败'
  } finally {
    loading.value = false
  }
}

function reset() {
  result.value = null
  error.value = ''
  sourceFile.value = null
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

onMounted(loadQuota)
onBeforeUnmount(() => {
  if (preview.value) URL.revokeObjectURL(preview.value)
})
</script>

<template>
  <div class="max-w-3xl mx-auto space-y-5">
    <div class="flex items-end justify-between gap-4">
      <div>
        <h2 class="text-xl font-bold">拍照搜题</h2>
        <p class="text-sm text-slate-500 mt-1">拍下题目，识别题干并生成答案和步骤。图片只用于本次识别，不入库。</p>
      </div>
      <div v-if="quota" class="text-xs text-slate-400 whitespace-nowrap">今日 AI 剩余 {{ quota.remaining }} 次</div>
    </div>

    <section class="bg-white rounded-2xl border border-slate-200 p-5">
      <input ref="fileInput" type="file" accept="image/*" capture="environment" class="hidden" @change="onFile" />

      <div v-if="!preview" class="border-2 border-dashed border-slate-200 rounded-xl py-12 text-center">
        <div class="text-4xl mb-2">📷</div>
        <p class="text-slate-600 text-sm">对准题目，拍清楚题干和选项</p>
        <button @click="chooseFile" class="mt-4 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium">
          拍照 / 选择图片
        </button>
      </div>

      <div v-else class="space-y-4">
        <img :src="preview" alt="题目预览" class="max-h-72 mx-auto rounded-lg border border-slate-200 object-contain" />
        <div class="flex justify-center gap-2">
          <button @click="chooseFile" class="px-4 py-2 border border-slate-300 rounded-lg text-sm">重选</button>
          <button @click="reset" class="px-4 py-2 border border-slate-300 rounded-lg text-sm">清除</button>
          <button @click="search" :disabled="loading"
                  class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium disabled:opacity-50">
            {{ loading ? '识别中…' : '开始搜题' }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="mt-4 text-sm text-slate-500 text-center">
        正在识别题目、生成解析，约 10～30 秒…
      </div>
      <div v-if="error" class="mt-4 p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm">
        {{ error }}
      </div>
    </section>

    <section v-if="result" class="bg-white rounded-2xl border border-slate-200 p-6 space-y-5">
      <div class="flex items-center justify-between gap-3">
        <h3 class="font-bold">识别结果</h3>
        <div class="flex items-center gap-2 text-xs">
          <span v-if="result.confidence" class="px-2 py-0.5 rounded bg-emerald-50 text-emerald-600">
            置信度 {{ Math.round(result.confidence * 100) }}%
          </span>
          <span class="text-slate-400">{{ result.model }}</span>
        </div>
      </div>

      <div v-if="!result.recognized" class="p-4 bg-amber-50 text-amber-800 rounded-lg text-sm">
        这张图里没有识别出数学题，换一张更清晰、只拍题目的图片试试。
      </div>

      <template v-else>
        <div v-if="result.content" class="text-slate-800 leading-8" v-html="renderMath(result.content)"></div>
        <div v-if="result.options?.length" class="grid sm:grid-cols-2 gap-2">
          <div v-for="(opt, i) in result.options" :key="i" class="px-3 py-2 bg-slate-50 rounded-lg text-sm text-slate-700">
            {{ opt }}
          </div>
        </div>

        <div v-if="result.answer" class="p-4 bg-indigo-50 border border-indigo-100 rounded-xl">
          <div class="text-xs text-indigo-500 mb-1">答案</div>
          <div class="text-slate-800" v-html="renderMath(result.answer)"></div>
        </div>

        <div v-if="result.steps?.length">
          <div class="text-sm font-semibold text-slate-700 mb-2">解析步骤</div>
          <ol class="space-y-2">
            <li v-for="(step, i) in result.steps" :key="i" class="flex gap-3 text-sm text-slate-700">
              <span class="shrink-0 w-6 h-6 rounded-full bg-slate-100 text-slate-500 text-xs flex items-center justify-center">{{ i + 1 }}</span>
              <div class="leading-7" v-html="renderMath(step)"></div>
            </li>
          </ol>
        </div>

        <div v-if="result.knowledgePoints?.length" class="flex flex-wrap gap-2">
          <span v-for="kp in result.knowledgePoints" :key="kp" class="px-2 py-1 bg-slate-100 text-slate-600 rounded text-xs">{{ kp }}</span>
        </div>

        <div v-if="result.warnings?.length" class="p-3 bg-amber-50 text-amber-800 rounded-lg text-xs space-y-1">
          <div v-for="w in result.warnings" :key="w">⚠ {{ w }}</div>
        </div>
      </template>

      <div class="flex gap-2 pt-2 border-t border-slate-100">
        <button @click="reset" class="px-4 py-2 bg-slate-800 text-white rounded-lg text-sm">再拍一题</button>
        <RouterLink to="/bank" class="px-4 py-2 border border-slate-300 rounded-lg text-sm">去题库练习</RouterLink>
      </div>
    </section>

    <p class="text-xs text-slate-400 leading-relaxed">
      提示：拍照搜题只做辅助，识别结果可能有误，请以题目原文和老师讲解为准。图片不会保存到题库。
    </p>
  </div>
</template>
