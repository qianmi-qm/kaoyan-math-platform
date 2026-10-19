<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { setAuth } from '../auth'

const router = useRouter()
const route = useRoute()

const mode = ref('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const error = ref('')
const loading = ref(false)

function switchMode(next) {
  mode.value = next
  error.value = ''
  password.value = ''
  confirmPassword.value = ''
}

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请填写用户名和密码'
    return
  }
  if (mode.value === 'register' && password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    const payload = { username: username.value.trim(), password: password.value }
    const data = mode.value === 'login'
      ? await api.login(payload)
      : await api.register(payload)
    setAuth(data)
    const redirect = typeof route.query.redirect === 'string'
      ? route.query.redirect
      : '/bank'
    router.replace(redirect === '/login' ? '/bank' : redirect)
  } catch (e) {
    error.value = e.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 flex items-center justify-center px-4">
    <div class="w-full max-w-md bg-white rounded-2xl border border-slate-200 shadow-sm p-8">
      <div class="mb-6">
        <div class="text-2xl font-bold text-slate-800">考研高数学习平台</div>
        <div class="text-sm text-slate-500 mt-1">登录后继续你的学习进度</div>
      </div>

      <div class="flex rounded-lg bg-slate-100 p-1 mb-6">
        <button
          type="button"
          class="flex-1 py-2 rounded-md text-sm font-medium transition"
          :class="mode === 'login' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-500'"
          @click="switchMode('login')"
        >登录</button>
        <button
          type="button"
          class="flex-1 py-2 rounded-md text-sm font-medium transition"
          :class="mode === 'register' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-500'"
          @click="switchMode('register')"
        >注册</button>
      </div>

      <form class="space-y-4" @submit.prevent="submit">
        <div>
          <label class="block text-sm text-slate-600 mb-1">用户名</label>
          <input
            v-model="username"
            type="text"
            autocomplete="username"
            placeholder="3~50 位字母、数字或下划线"
            class="w-full px-3 py-2.5 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-200 focus:border-indigo-400"
          />
        </div>
        <div>
          <label class="block text-sm text-slate-600 mb-1">密码</label>
          <input
            v-model="password"
            type="password"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            placeholder="至少 6 位"
            class="w-full px-3 py-2.5 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-200 focus:border-indigo-400"
          />
        </div>
        <div v-if="mode === 'register'">
          <label class="block text-sm text-slate-600 mb-1">确认密码</label>
          <input
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="再输入一次"
            class="w-full px-3 py-2.5 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-200 focus:border-indigo-400"
          />
        </div>

        <div v-if="error" class="text-sm text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2">
          {{ error }}
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition disabled:opacity-50"
        >{{ loading ? '请稍候…' : (mode === 'login' ? '登录' : '注册并登录') }}</button>
      </form>

      <div class="mt-5 text-xs text-slate-400 leading-relaxed">
        演示账号由部署者通过 <span class="font-mono">DEMO_USER_ENABLED / DEMO_USER_PASSWORD</span> 配置。<br />
        新注册账号从空题库视角开始，学习记录互相隔离。
      </div>
    </div>
  </div>
</template>
