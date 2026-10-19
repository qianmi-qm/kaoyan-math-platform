<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../api'
import { renderMath } from '../utils/math'

const cameraInput = ref(null)
const galleryInput = ref(null)
const sourceFile = ref(null)
const preview = ref('')
const result = ref(null)
const error = ref('')
const loading = ref(false)
const quota = ref(null)
const dragging = ref(false)
const favoriteLoading = ref(false)

async function loadQuota() {
  try {
    quota.value = await api.aiQuota()
  } catch {
    quota.value = null
  }
}

function chooseCamera() {
  cameraInput.value?.click()
}

function chooseGallery() {
  galleryInput.value?.click()
}

function setFile(file) {
  if (!file) return
  error.value = ''
  result.value = null
  sourceFile.value = file
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = URL.createObjectURL(file)
}

function onFile(event) {
  setFile(event.target.files?.[0])
  event.target.value = ''
}

function onDrop(event) {
  dragging.value = false
  setFile(event.dataTransfer?.files?.[0])
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

async function toggleFavorite() {
  if (!result.value?.recordId || favoriteLoading.value) return
  favoriteLoading.value = true
  try {
    const updated = await api.photoFavorite(result.value.recordId, !result.value.favorite)
    result.value.favorite = updated.favorite
  } catch (e) {
    error.value = e.message
  } finally {
    favoriteLoading.value = false
  }
}

function reset() {
  result.value = null
  error.value = ''
  sourceFile.value = null
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = ''
  if (cameraInput.value) cameraInput.value.value = ''
  if (galleryInput.value) galleryInput.value.value = ''
}

onMounted(loadQuota)
onBeforeUnmount(() => {
  if (preview.value) URL.revokeObjectURL(preview.value)
})
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <div class="eyebrow">AI Vision</div>
        <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">拍照搜题</h1>
        <p class="mt-2 max-w-xl text-sm leading-6 text-slate-500">拍下题目，识别题干与公式，给出答案和分步解析。图片只用于本次识别，不会写入题库。</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <div v-if="quota" class="chip bg-white text-slate-500 ring-1 ring-slate-200">
          <span class="mr-1 h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
          今日 AI 剩余 {{ quota.remaining }} 次
        </div>
        <RouterLink to="/photo/history" class="btn-secondary">搜题历史</RouterLink>
      </div>
    </header>

    <div class="flex items-center gap-2 text-[11px] text-slate-400 sm:gap-3">
      <span class="flex items-center gap-1.5"><b class="grid h-5 w-5 place-items-center rounded-full bg-indigo-600 text-[10px] text-white">1</b>拍照</span>
      <span class="h-px w-6 bg-slate-200 sm:w-10"></span>
      <span class="flex items-center gap-1.5" :class="loading || result ? 'text-indigo-600' : ''"><b class="grid h-5 w-5 place-items-center rounded-full bg-slate-100 text-[10px] text-slate-500" :class="loading || result ? '!bg-indigo-600 !text-white' : ''">2</b>识别</span>
      <span class="h-px w-6 bg-slate-200 sm:w-10"></span>
      <span class="flex items-center gap-1.5" :class="result ? 'text-indigo-600' : ''"><b class="grid h-5 w-5 place-items-center rounded-full bg-slate-100 text-[10px] text-slate-500" :class="result ? '!bg-indigo-600 !text-white' : ''">3</b>解析</span>
    </div>

    <section
      class="app-card relative overflow-hidden p-5 sm:p-7"
      :class="dragging ? 'border-indigo-400 ring-4 ring-indigo-500/10' : ''"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
    >
      <input ref="cameraInput" type="file" accept="image/*" capture="environment" class="hidden" @change="onFile" />
      <input ref="galleryInput" type="file" accept="image/*" class="hidden" @change="onFile" />

      <div v-if="!preview" class="relative rounded-2xl border border-dashed border-slate-200 bg-slate-50/60 px-5 py-12 text-center sm:py-16">
        <div class="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-white text-3xl shadow-sm ring-1 ring-slate-100">◉</div>
        <h2 class="mt-5 text-base font-semibold text-slate-800">把题目放进取景框</h2>
        <p class="mx-auto mt-1.5 max-w-sm text-xs leading-5 text-slate-400">保持光线充足，题干和选项完整入镜；支持 JPG / PNG / WebP。</p>
        <div class="mt-6 flex flex-wrap justify-center gap-2">
          <button class="btn-primary" @click="chooseCamera">
            <span>📷</span>拍照搜题
          </button>
          <button class="btn-secondary" @click="chooseGallery">从相册选择</button>
        </div>
        <p class="mt-4 text-[10px] text-slate-400">也可以直接把图片拖进来</p>
      </div>

      <div v-else class="space-y-4">
        <div class="relative overflow-hidden rounded-2xl bg-slate-900">
          <img :src="preview" alt="题目预览" class="mx-auto max-h-[420px] w-auto object-contain opacity-95" />
          <div v-if="loading" class="absolute inset-0 bg-slate-950/30">
            <div class="scan-line absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-transparent via-indigo-300 to-transparent shadow-[0_0_24px_rgba(129,140,248,.9)]"></div>
          </div>
          <div v-if="loading" class="absolute inset-x-0 bottom-0 bg-gradient-to-t from-slate-950/80 to-transparent p-5 text-center">
            <div class="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs text-white backdrop-blur">
              <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-300"></span>
              正在识别题目并生成解析…
            </div>
          </div>
        </div>

        <div class="flex flex-wrap justify-center gap-2">
          <button class="btn-secondary" :disabled="loading" @click="chooseCamera">重拍</button>
          <button class="btn-secondary" :disabled="loading" @click="chooseGallery">换一张</button>
          <button class="btn-primary min-w-32" :disabled="loading" @click="search">
            {{ loading ? '识别中…' : '开始搜题' }}
          </button>
        </div>
      </div>

      <div v-if="error" class="mt-4 flex items-start gap-2 rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-600">
        <span class="mt-0.5">!</span><span>{{ error }}</span>
      </div>
    </section>

    <section v-if="result" class="space-y-5 fade-up">
      <div class="app-card overflow-hidden">
        <div class="border-b border-slate-100 bg-gradient-to-br from-indigo-50/80 via-white to-violet-50/40 px-5 py-5 sm:px-7">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="flex items-center gap-2">
              <span class="grid h-8 w-8 place-items-center rounded-xl bg-white text-indigo-600 shadow-sm ring-1 ring-indigo-100">✓</span>
              <div>
                <div class="text-sm font-semibold text-slate-800">{{ result.recognized ? '识别完成' : '未识别到题目' }}</div>
                <div class="text-[11px] text-slate-400">模型 {{ result.model }} · 仅辅助，请核对题干</div>
              </div>
            </div>
            <span v-if="result.confidence" class="chip bg-white text-emerald-600 ring-1 ring-emerald-100">
              置信度 {{ Math.round(result.confidence * 100) }}%
            </span>
          </div>
        </div>

        <div class="space-y-6 p-5 sm:p-7">
          <div v-if="!result.recognized" class="rounded-xl border border-amber-100 bg-amber-50 p-4 text-sm text-amber-800">
            这张图里没有识别出数学题。换一张更清晰、只拍题目的图片试试。
          </div>

          <template v-else>
            <div v-if="result.content" class="text-[17px] leading-8 text-slate-800" v-html="renderMath(result.content)"></div>

            <div v-if="result.options?.length" class="grid gap-2 sm:grid-cols-2">
              <div v-for="(opt, i) in result.options" :key="i" class="rounded-xl border border-slate-100 bg-slate-50/70 px-3.5 py-2.5 text-sm text-slate-700">
                {{ opt }}
              </div>
            </div>

            <div v-if="result.answer" class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-600 to-violet-600 p-5 text-white shadow-lg shadow-indigo-500/20">
              <div class="absolute -right-6 -top-8 text-8xl font-serif text-white/10">∑</div>
              <div class="text-[11px] font-medium uppercase tracking-[0.18em] text-indigo-100/80">Answer</div>
              <div class="mt-2 text-lg font-semibold leading-8" v-html="renderMath(result.answer)"></div>
            </div>

            <div v-if="result.steps?.length">
              <div class="mb-3 flex items-center gap-2">
                <span class="text-sm font-semibold text-slate-800">解析步骤</span>
                <span class="chip bg-slate-100 text-slate-400">{{ result.steps.length }} 步</span>
              </div>
              <ol class="space-y-3">
                <li v-for="(step, i) in result.steps" :key="i" class="flex gap-3">
                  <span class="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-indigo-50 text-[11px] font-semibold text-indigo-600">{{ i + 1 }}</span>
                  <div class="min-w-0 pt-0.5 text-sm leading-7 text-slate-700" v-html="renderMath(step)"></div>
                </li>
              </ol>
            </div>

            <div v-if="result.knowledgePoints?.length" class="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              <span v-for="kp in result.knowledgePoints" :key="kp" class="chip bg-slate-100 text-slate-500"># {{ kp }}</span>
            </div>

            <div v-if="result.warnings?.length" class="space-y-1 rounded-xl border border-amber-100 bg-amber-50 p-3.5 text-xs leading-5 text-amber-800">
              <div v-for="w in result.warnings" :key="w">⚠ {{ w }}</div>
            </div>
          </template>
        </div>
      </div>

      <div class="flex flex-wrap gap-2">
        <button class="btn-primary" @click="reset">再拍一题</button>
        <button
          v-if="result.recordId"
          class="btn-secondary"
          :disabled="favoriteLoading"
          @click="toggleFavorite"
        >{{ result.favorite ? '★ 已收藏' : '☆ 收藏到错题本' }}</button>
        <RouterLink to="/photo/history" class="btn-secondary">查看搜题历史</RouterLink>
        <RouterLink to="/bank" class="btn-secondary">去题库练习</RouterLink>
      </div>
    </section>

    <p class="px-1 text-[11px] leading-5 text-slate-400">
      提示：拍照搜题只做辅助，识别结果可能有误，请以题目原文和老师讲解为准。图片不会保存到题库。
    </p>
  </div>
</template>
