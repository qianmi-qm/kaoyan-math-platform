<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const route = useRoute()
const token = typeof route.query.token === 'string' ? route.query.token : ''
const status = ref('验证中…')
const ok = ref(false)

onMounted(async () => {
  if (!token) {
    status.value = '链接缺少 token'
    return
  }
  try {
    await api.verifyEmail({ token })
    ok.value = true
    status.value = '邮箱验证成功'
  } catch (e) {
    status.value = e.message
  }
})
</script>

<template>
  <div class="min-h-screen bg-slate-50 flex items-center justify-center px-4">
    <div class="w-full max-w-md bg-white rounded-2xl border border-slate-200 p-8 text-center">
      <h1 class="text-xl font-bold mb-2">邮箱验证</h1>
      <p :class="ok ? 'text-emerald-600' : 'text-slate-600'">{{ status }}</p>
      <button class="mt-5 text-sm text-indigo-600" @click="router.replace('/login')">返回登录</button>
    </div>
  </div>
</template>
