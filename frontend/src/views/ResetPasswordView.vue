<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const route = useRoute()
const token = typeof route.query.token === 'string' ? route.query.token : ''
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const message = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (!token) {
    error.value = '链接缺少 token'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await api.resetPassword({ token, newPassword: newPassword.value })
    message.value = '密码已重置，正在返回登录…'
    setTimeout(() => router.replace('/login'), 900)
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
        <div class="eyebrow">New password</div>
        <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">重置密码</h1>
        <p class="mt-2 text-sm text-slate-500">链接 30 分钟内有效，只能使用一次。</p>
        <form class="mt-6 space-y-4" @submit.prevent="submit">
          <input v-model="newPassword" type="password" placeholder="新密码（至少 6 位）" autocomplete="new-password" class="input-field" />
          <input v-model="confirmPassword" type="password" placeholder="确认新密码" autocomplete="new-password" class="input-field" />
          <div v-if="error" class="rounded-xl border border-rose-100 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-600">{{ error }}</div>
          <div v-if="message" class="rounded-xl border border-emerald-100 bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">{{ message }}</div>
          <button :disabled="loading" class="btn-primary w-full py-3">
            {{ loading ? '提交中…' : '重置密码' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
