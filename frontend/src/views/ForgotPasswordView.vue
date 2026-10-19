<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const email = ref('')
const message = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  message.value = ''
  if (!email.value.trim()) {
    error.value = '请输入邮箱'
    return
  }
  loading.value = true
  try {
    await api.forgotPassword({ email: email.value.trim() })
    message.value = '如果邮箱存在，重置链接已发送；本地未配 SMTP 时会打印在 backend 日志。'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 flex items-center justify-center px-4">
    <div class="w-full max-w-md bg-white rounded-2xl border border-slate-200 p-8">
      <h1 class="text-xl font-bold mb-1">忘记密码</h1>
      <p class="text-sm text-slate-500 mb-5">输入注册邮箱，我们会发送一次性重置链接。</p>
      <form class="space-y-4" @submit.prevent="submit">
        <input v-model="email" type="email" placeholder="邮箱" class="w-full px-3 py-2.5 rounded-lg border border-slate-300" />
        <div v-if="error" class="text-sm text-rose-600">{{ error }}</div>
        <div v-if="message" class="text-sm text-emerald-700 bg-emerald-50 rounded-lg px-3 py-2">{{ message }}</div>
        <button :disabled="loading" class="w-full py-2.5 bg-indigo-600 text-white rounded-lg disabled:opacity-50">
          {{ loading ? '发送中…' : '发送重置链接' }}
        </button>
      </form>
      <button class="mt-4 text-sm text-slate-500 hover:text-indigo-600" @click="router.replace('/login')">返回登录</button>
    </div>
  </div>
</template>
