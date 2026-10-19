<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { authState, clearAuth, updateUser } from '../auth'

const router = useRouter()
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
  <div class="max-w-xl mx-auto bg-white rounded-xl border border-slate-200 p-6">
    <div class="mb-6 pb-5 border-b border-slate-100">
      <h3 class="text-base font-semibold mb-1">邮箱</h3>
      <p class="text-sm text-slate-500 mb-3">
        当前：{{ authState.user?.email || '未设置' }}
        <span v-if="authState.user?.email" :class="authState.user.emailVerified ? 'text-emerald-600' : 'text-amber-600'">
          {{ authState.user.emailVerified ? '（已验证）' : '（未验证）' }}
        </span>
      </p>
      <div class="flex gap-2">
        <input v-model="email" type="email" placeholder="you@example.com"
               class="flex-1 px-3 py-2 rounded-lg border border-slate-300 text-sm" />
        <button :disabled="emailLoading" @click="saveEmail"
                class="px-3 py-2 bg-slate-800 text-white rounded-lg text-sm disabled:opacity-50">保存并验证</button>
        <button v-if="authState.user?.email && !authState.user?.emailVerified" @click="resend"
                class="px-3 py-2 border border-slate-300 rounded-lg text-sm">重发</button>
      </div>
      <div v-if="emailError" class="mt-2 text-sm text-rose-600">{{ emailError }}</div>
      <div v-if="emailSuccess" class="mt-2 text-sm text-emerald-700">{{ emailSuccess }}</div>
    </div>

    <h2 class="text-lg font-bold mb-1">修改密码</h2>
    <p class="text-sm text-slate-500 mb-5">修改成功后，当前账号所有旧 token 会立即失效。</p>

    <form class="space-y-4" @submit.prevent="submit">
      <div>
        <label class="block text-sm text-slate-600 mb-1">原密码</label>
        <input v-model="oldPassword" type="password" autocomplete="current-password"
               class="w-full px-3 py-2.5 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
      </div>
      <div>
        <label class="block text-sm text-slate-600 mb-1">新密码</label>
        <input v-model="newPassword" type="password" autocomplete="new-password" placeholder="至少 6 位"
               class="w-full px-3 py-2.5 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
      </div>
      <div>
        <label class="block text-sm text-slate-600 mb-1">确认新密码</label>
        <input v-model="confirmPassword" type="password" autocomplete="new-password"
               class="w-full px-3 py-2.5 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
      </div>

      <div v-if="error" class="text-sm text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2">{{ error }}</div>
      <div v-if="success" class="text-sm text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-lg px-3 py-2">{{ success }}</div>

      <button type="submit" :disabled="loading"
              class="w-full py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition disabled:opacity-50">
        {{ loading ? '提交中…' : '修改密码' }}
      </button>
    </form>
  </div>
</template>
