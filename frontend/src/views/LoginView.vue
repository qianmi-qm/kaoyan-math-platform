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
const email = ref('')
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
    if (mode.value === 'register' && email.value.trim()) {
      payload.email = email.value.trim()
    }
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
  <div class="min-h-screen bg-slate-50 lg:grid lg:grid-cols-[1.05fr_0.95fr]">
    <!-- 品牌区：桌面端 -->
    <section class="relative hidden overflow-hidden bg-[#0d1024] p-12 text-white lg:flex lg:flex-col lg:justify-between">
      <div class="absolute inset-0 grid-pattern opacity-40"></div>
      <div class="absolute -left-24 -top-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl"></div>
      <div class="absolute -bottom-32 right-0 h-96 w-96 rounded-full bg-violet-500/15 blur-3xl"></div>

      <div class="relative">
        <div class="flex items-center gap-3">
          <span class="grid h-11 w-11 place-items-center rounded-2xl bg-white/10 text-2xl font-serif ring-1 ring-white/15">∫</span>
          <div>
            <div class="text-lg font-bold tracking-tight">研途高数</div>
            <div class="text-[10px] uppercase tracking-[0.24em] text-indigo-200/60">Math Lab</div>
          </div>
        </div>

        <div class="mt-24 max-w-lg">
          <div class="eyebrow !text-indigo-300">考研数学 · 智能学习空间</div>
          <h1 class="mt-5 text-5xl font-bold leading-[1.15] tracking-tight text-balance">
            把每一道错题，<br />
            变成下一次<span class="bg-gradient-to-r from-indigo-300 to-violet-300 bg-clip-text text-transparent">得分</span>。
          </h1>
          <p class="mt-6 max-w-md text-[15px] leading-8 text-slate-300/80">
            题库、练习、日报、艾宾浩斯复习和 AI 拍照搜题，放在一个安静的工作台里。
          </p>
        </div>

        <div class="mt-12 max-w-md rounded-2xl border border-white/10 bg-white/[0.04] p-5 backdrop-blur">
          <div class="flex items-center justify-between text-xs text-indigo-200/60">
            <span>今日专注</span>
            <span>DAY 01</span>
          </div>
          <div class="mt-3 font-serif text-2xl tracking-wide text-white/90">
            lim<sub class="text-base">x→0</sub> sin x / x = 1
          </div>
          <div class="mt-4 flex gap-2 text-[11px] text-slate-300/70">
            <span class="chip bg-white/10 text-indigo-100">极限</span>
            <span class="chip bg-white/10 text-indigo-100">重要极限</span>
            <span class="chip bg-white/10 text-indigo-100">0/0 型</span>
          </div>
        </div>
      </div>

      <div class="relative flex items-center gap-6 text-[11px] text-slate-400">
        <span>题库练习</span>
        <span class="h-1 w-1 rounded-full bg-slate-600"></span>
        <span>AI 拍照搜题</span>
        <span class="h-1 w-1 rounded-full bg-slate-600"></span>
        <span>遗忘曲线复习</span>
      </div>
    </section>

    <!-- 表单区 -->
    <section class="relative flex min-h-screen items-center justify-center px-4 py-10 sm:px-8">
      <div class="absolute inset-0 app-bg lg:hidden"></div>
      <div class="relative w-full max-w-[420px]">
        <div class="mb-8 flex items-center gap-2.5 lg:hidden">
          <span class="grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-xl font-serif text-white shadow-lg shadow-indigo-500/25">∫</span>
          <div>
            <div class="text-base font-bold text-slate-900">研途高数</div>
            <div class="text-[9px] uppercase tracking-[0.22em] text-slate-400">Math Lab</div>
          </div>
        </div>

        <div class="app-card p-7 sm:p-9">
          <div class="mb-7">
            <div class="eyebrow">{{ mode === 'login' ? 'Welcome back' : 'Create account' }}</div>
            <h2 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">
              {{ mode === 'login' ? '继续今天的学习' : '创建你的学习空间' }}
            </h2>
            <p class="mt-2 text-sm text-slate-500">
              {{ mode === 'login' ? '登录后继续你的学习进度' : '注册即开始，记录只属于你' }}
            </p>
          </div>

          <div class="mb-6 grid grid-cols-2 rounded-xl bg-slate-100 p-1">
            <button
              type="button"
              class="rounded-lg py-2 text-sm font-semibold transition"
              :class="mode === 'login' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
              @click="switchMode('login')"
            >登录</button>
            <button
              type="button"
              class="rounded-lg py-2 text-sm font-semibold transition"
              :class="mode === 'register' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
              @click="switchMode('register')"
            >注册</button>
          </div>

          <form class="space-y-4" @submit.prevent="submit">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-slate-600">用户名</label>
              <input
                v-model="username"
                type="text"
                autocomplete="username"
                placeholder="3~50 位字母、数字或下划线"
                class="input-field"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-slate-600">密码</label>
              <input
                v-model="password"
                type="password"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
                placeholder="至少 6 位"
                class="input-field"
              />
            </div>
            <div v-if="mode === 'register'">
              <label class="mb-1.5 block text-xs font-medium text-slate-600">邮箱 <span class="font-normal text-slate-400">可选，用于验证 / 找回密码</span></label>
              <input
                v-model="email"
                type="email"
                autocomplete="email"
                placeholder="you@example.com"
                class="input-field"
              />
            </div>
            <div v-if="mode === 'register'">
              <label class="mb-1.5 block text-xs font-medium text-slate-600">确认密码</label>
              <input
                v-model="confirmPassword"
                type="password"
                autocomplete="new-password"
                placeholder="再输入一次"
                class="input-field"
              />
            </div>

            <div v-if="error" class="rounded-xl border border-rose-100 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-600">
              {{ error }}
            </div>

            <button type="submit" :disabled="loading" class="btn-primary mt-1 w-full py-3">
              {{ loading ? '请稍候…' : (mode === 'login' ? '进入学习空间' : '注册并开始') }}
            </button>
          </form>

          <div v-if="mode === 'login'" class="mt-4 text-center">
            <RouterLink to="/forgot-password" class="text-xs text-slate-400 transition hover:text-indigo-600">忘记密码？</RouterLink>
          </div>
        </div>

        <p class="mt-5 text-center text-[11px] leading-5 text-slate-400">
          演示账号由部署者通过 <span class="font-mono text-slate-500">DEMO_USER_ENABLED</span> 配置<br />
          新注册账号从空题库视角开始，学习记录互相隔离
        </p>
      </div>
    </section>
  </div>
</template>
