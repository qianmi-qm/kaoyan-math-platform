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
  <div class="app-bg flex min-h-screen items-center justify-center px-4 py-10">
    <div class="w-full max-w-md">
      <div class="mb-7 flex items-center gap-2.5">
        <span class="grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-xl font-serif text-white shadow-lg shadow-indigo-500/25">∫</span>
        <div>
          <div class="text-base font-bold text-slate-900">研途高数</div>
          <div class="text-[9px] uppercase tracking-[0.22em] text-slate-400">Math Lab</div>
        </div>
      </div>

      <div class="app-card p-9 text-center">
        <div class="mx-auto grid h-14 w-14 place-items-center rounded-2xl" :class="ok ? 'bg-emerald-50 text-2xl text-emerald-500' : 'bg-slate-50 text-2xl text-slate-400'">
          {{ ok ? '✓' : '✉' }}
        </div>
        <h1 class="mt-4 text-2xl font-bold tracking-tight text-slate-900">邮箱验证</h1>
        <p class="mt-2 text-sm" :class="ok ? 'text-emerald-600' : 'text-slate-500'">{{ status }}</p>
        <button class="btn-secondary mt-6 w-full" @click="router.replace('/login')">返回登录</button>
      </div>
    </div>
  </div>
</template>
