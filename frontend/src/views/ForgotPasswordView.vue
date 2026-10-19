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
    message.value = '如果该邮箱已注册，重置链接已发送，请查收。'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="app-bg flex min-h-screen items-center justify-center px-4 py-10">
    <div class="w-full max-w-md">
      <div class="mb-7 flex items-center gap-2.5">
        <span class="grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-xl font-serif text-white shadow-lg shadow-indigo-500/25">∫</span>
        <div>
          <div class="text-base font-bold text-slate-900">研途高数</div>
          <div class="text-[9px] uppercase tracking-[0.22em] text-slate-400">Math Lab</div>
        </div>
      </div>

      <div class="app-card p-7 sm:p-9">
        <div class="eyebrow">Account recovery</div>
        <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">忘记密码</h1>
        <p class="mt-2 text-sm leading-6 text-slate-500">输入注册邮箱，我们会发送一条 30 分钟内有效的一次性重置链接。</p>

        <form class="mt-6 space-y-4" @submit.prevent="submit">
          <input v-model="email" type="email" placeholder="you@example.com" class="input-field" />
          <div v-if="error" class="rounded-xl border border-rose-100 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-600">{{ error }}</div>
          <div v-if="message" class="rounded-xl border border-emerald-100 bg-emerald-50 px-3.5 py-2.5 text-sm leading-6 text-emerald-700">{{ message }}</div>
          <button :disabled="loading" class="btn-primary w-full py-3">
            {{ loading ? '发送中…' : '发送重置链接' }}
          </button>
        </form>

        <button class="mt-4 w-full text-center text-xs text-slate-400 hover:text-indigo-600" @click="router.replace('/login')">返回登录</button>
      </div>
    </div>
  </div>
</template>
