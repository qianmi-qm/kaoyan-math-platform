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
  <div class="min-h-screen bg-slate-50 flex items-center justify-center px-4">
    <div class="w-full max-w-md bg-white rounded-2xl border border-slate-200 p-8">
      <h1 class="text-xl font-bold mb-1">重置密码</h1>
      <p class="text-sm text-slate-500 mb-5">链接 30 分钟内有效，只能使用一次。</p>
      <form class="space-y-4" @submit.prevent="submit">
        <input v-model="newPassword" type="password" placeholder="新密码（至少 6 位）" autocomplete="new-password"
               class="w-full px-3 py-2.5 rounded-lg border border-slate-300" />
        <input v-model="confirmPassword" type="password" placeholder="确认新密码" autocomplete="new-password"
               class="w-full px-3 py-2.5 rounded-lg border border-slate-300" />
        <div v-if="error" class="text-sm text-rose-600">{{ error }}</div>
        <div v-if="message" class="text-sm text-emerald-700 bg-emerald-50 rounded-lg px-3 py-2">{{ message }}</div>
        <button :disabled="loading" class="w-full py-2.5 bg-indigo-600 text-white rounded-lg disabled:opacity-50">
          {{ loading ? '提交中…' : '重置密码' }}
        </button>
      </form>
    </div>
  </div>
</template>
