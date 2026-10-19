<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api'
import { renderMath } from '../utils/math'

const questions = ref([])
const kps = ref([])
const total = ref(0)
const error = ref('')
const message = ref('')
const editingId = ref(null)

const form = reactive({
  type: 1,
  content: '',
  options: '',
  answer: '',
  analysis: '',
  difficulty: 2,
  kpIds: '',
})

async function load() {
  error.value = ''
  try {
    const data = await api.listQuestions({ page: 1, size: 30 })
    questions.value = data.records
    total.value = data.total
    kps.value = await api.listKnowledgePoints()
  } catch (e) {
    error.value = e.message
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { type: 1, content: '', options: '', answer: '', analysis: '', difficulty: 2, kpIds: '' })
}

async function edit(q) {
  error.value = ''
  try {
    const detail = await api.questionDetail(q.id)
    editingId.value = q.id
    Object.assign(form, {
      type: detail.type,
      content: detail.content,
      options: detail.options || '',
      answer: detail.answer,
      analysis: detail.analysis || '',
      difficulty: detail.difficulty,
      kpIds: (detail.kpIds || []).join(','),
    })
  } catch (e) {
    error.value = e.message
  }
}

async function submit() {
  error.value = ''
  message.value = ''
  const kpIds = form.kpIds.split(',').map(x => Number(x.trim())).filter(Boolean)
  if (!kpIds.length) {
    error.value = '至少填一个知识点 id'
    return
  }
  const payload = {
    type: Number(form.type),
    content: form.content,
    options: form.options || null,
    answer: form.answer,
    analysis: form.analysis || null,
    difficulty: Number(form.difficulty),
    kpIds,
  }
  try {
    if (editingId.value) {
      await api.updateQuestion(editingId.value, payload)
      message.value = '题目已更新'
    } else {
      await api.createQuestion(payload)
      message.value = '题目已创建'
    }
    resetForm()
    await load()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-lg font-bold">题库管理</h2>
        <p class="text-sm text-slate-500 mt-1">共 {{ total }} 题；可新增或编辑，暂不支持删除。</p>
      </div>
      <button class="px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-slate-50" @click="resetForm">新增题目</button>
    </div>

    <div v-if="error" class="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm">{{ error }}</div>
    <div v-if="message" class="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg text-sm">{{ message }}</div>

    <section class="bg-white rounded-xl border border-slate-200 p-5">
      <div class="text-sm font-medium text-slate-700 mb-3">{{ editingId ? `编辑题目 #${editingId}` : '新增题目' }}</div>
      <div class="grid md:grid-cols-2 gap-3">
        <select v-model="form.type" class="px-3 py-2 border border-slate-300 rounded-lg text-sm">
          <option :value="1">单选</option>
          <option :value="2">填空</option>
          <option :value="3">大题</option>
        </select>
        <select v-model="form.difficulty" class="px-3 py-2 border border-slate-300 rounded-lg text-sm">
          <option :value="1">易</option>
          <option :value="2">中</option>
          <option :value="3">难</option>
        </select>
        <input v-model="form.kpIds" placeholder="知识点 id，逗号分隔，如 1,2" class="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
        <input v-model="form.answer" placeholder="正确答案" class="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
      </div>
      <textarea v-model="form.content" rows="3" placeholder="题干（支持 $...$ 公式）" class="mt-3 w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"></textarea>
      <input v-model="form.options" placeholder='选项 JSON 数组，如 ["A. 0","B. 1"]' class="mt-3 w-full px-3 py-2 border border-slate-300 rounded-lg text-sm" />
      <textarea v-model="form.analysis" rows="2" placeholder="解析（可选）" class="mt-3 w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"></textarea>
      <div class="mt-3 flex gap-2">
        <button @click="submit" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium">保存</button>
        <button v-if="editingId" @click="resetForm" class="px-4 py-2 border border-slate-300 rounded-lg text-sm">取消编辑</button>
      </div>
    </section>

    <section class="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100">
      <div v-for="q in questions" :key="q.id" class="p-4 flex items-start justify-between gap-4">
        <div class="min-w-0">
          <div class="text-xs text-slate-400 mb-1">#{{ q.id }} · {{ q.typeLabel }} · {{ q.difficultyLabel }}</div>
          <div class="text-sm text-slate-700" v-html="renderMath(q.content)"></div>
        </div>
        <button @click="edit(q)" class="text-xs text-indigo-600 hover:text-indigo-800 shrink-0">编辑</button>
      </div>
    </section>
  </div>
</template>
