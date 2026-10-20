<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { authState, clearAuth, updateUser } from '../auth'

const router = useRouter()
const initial = computed(() => (authState.user?.username || '?').slice(0, 1).toUpperCase())
const roleLabel = computed(() => (authState.user?.role === 'ADMIN' ? '管理员' : '学习者'))
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

const email = ref(authState.user?.email || '')
const emailError = ref('')
const emailSuccess = ref('')
const emailLoading = ref(false)

async function saveEmail() {
  emailError.value = ''
  emailSuccess.value = ''
  if (!email.value.trim()) {
    emailError.value = '请输入邮箱'
    return
  }
  emailLoading.value = true
  try {
    await api.updateEmail({ email: email.value.trim() })
    updateUser({ email: email.value.trim(), emailVerified: false })
    emailSuccess.value = '验证邮件已发送，请查收'
  } catch (e) {
    emailError.value = e.message
  } finally {
    emailLoading.value = false
  }
}

async function resend() {
  emailError.value = ''
  emailSuccess.value = ''
  try {
    await api.resendVerification()
    emailSuccess.value = '验证邮件已重发'
  } catch (e) {
    emailError.value = e.message
  }
}

async function submit() {
  error.value = ''
  success.value = ''
  if (!oldPassword.value || !newPassword.value) {
    error.value = '请填写原密码和新密码'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的新密码不一致'
    return
  }
  loading.value = true
  try {
    await api.changePassword({ oldPassword: oldPassword.value, newPassword: newPassword.value })
    success.value = '密码已修改，请重新登录'
    clearAuth()
    setTimeout(() => router.replace('/login'), 800)
  } catch (e) {
    error.value = e.message || '修改失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-xl space-y-5">
    <header class="flex items-center gap-4 rounded-2xl bg-gradient-to-br from-slate-900 to-indigo-950 p-5 text-white shadow-lg shadow-indigo-950/20">
      <span class="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-white/10 text-xl font-bold ring-1 ring-white/15">{{ initial }}</span>
      <div class="min-w-0">
        <div class="truncate text-lg font-bold">{{ authState.user?.username || '同学' }}</div>
        <div class="mt-0.5 text-xs text-indigo-200/70">{{ roleLabel }} · {{ authState.user?.email || '未绑定邮箱' }}</div>
      </div>
    </header>

    <div class="grid grid-cols-2 gap-2 sm:grid-cols-4">
      <RouterLink to="/report" class="app-card flex flex-col items-center gap-1.5 py-3 text-xs font-medium text-slate-600 hover:border-indigo-200">
        <span class="text-lg">◔</span>学习报告
      </RouterLink>
      <RouterLink to="/review" class="app-card flex flex-col items-center gap-1.5 py-3 text-xs font-medium text-slate-600 hover:border-indigo-200">
        <span class="text-lg">↻</span>今日复习
      </RouterLink>
      <RouterLink to="/photo" class="app-card flex flex-col items-center gap-1.5 py-3 text-xs font-medium text-slate-600 hover:border-indigo-200">
        <span class="text-lg">◉</span>拍照搜题
      </RouterLink>
      <RouterLink to="/bank" class="app-card flex flex-col items-center gap-1.5 py-3 text-xs font-medium text-slate-600 hover:border-indigo-200">
        <span class="text-lg">∑</span>题库
      </RouterLink>
    </div>

    <section class="app-card p-5 sm:p-6">
      <h3 class="text-base font-semibold text-slate-800">邮箱</h3>
      <p class="mt-1 text-sm text-slate-500">
        当前：{{ authState.user?.email || '未设置' }}
        <span v-if="authState.user?.email" :class="authState.user.emailVerified ? 'text-emerald-600' : 'text-amber-600'">
          {{ authState.user.emailVerified ? '（已验证）' : '（未验证）' }}
        </span>
      </p>
      <div class="mt-4 flex flex-col gap-2 sm:flex-row">
        <input v-model="email" type="email" placeholder="you@example.com" class="input-field flex-1" />
        <button :disabled="emailLoading" @click="saveEmail" class="btn-secondary whitespace-nowrap">{{ emailLoading ? '保存中…' : '保存并验证' }}</button>
        <button v-if="authState.user?.email && !authState.user?.emailVerified" @click="resend" class="btn-secondary whitespace-nowrap">重发</button>
      </div>
      <div v-if="emailError" class="mt-2 text-sm text-rose-600">{{ emailError }}</div>
      <div v-if="emailSuccess" class="mt-2 text-sm text-emerald-700">{{ emailSuccess }}</div>
    </section>

    <section class="app-card p-5 sm:p-6">
      <h2 class="text-base font-semibold text-slate-800">修改密码</h2>
      <p class="mt-1 text-sm text-slate-500">修改成功后，当前账号所有旧 token 会立即失效。</p>

      <form class="mt-5 space-y-4" @submit.prevent="submit">
        <div>
          <label class="mb-1.5 block text-xs font-medium text-slate-600">原密码</label>
          <input v-model="oldPassword" type="password" autocomplete="current-password" class="input-field" />
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-medium text-slate-600">新密码</label>
          <input v-model="newPassword" type="password" autocomplete="new-password" placeholder="至少 6 位" class="input-field" />
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-medium text-slate-600">确认新密码</label>
          <input v-model="confirmPassword" type="password" autocomplete="new-password" class="input-field" />
        </div>

        <div v-if="error" class="rounded-xl border border-rose-100 bg-rose-50 px-3 py-2 text-sm text-rose-600">{{ error }}</div>
        <div v-if="success" class="rounded-xl border border-emerald-100 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{{ success }}</div>

        <button type="submit" :disabled="loading" class="btn-primary w-full py-3">
          {{ loading ? '提交中…' : '修改密码' }}
        </button>
      </form>
    </section>
  </div>
</template>
