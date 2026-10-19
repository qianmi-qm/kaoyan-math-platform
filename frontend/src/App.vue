<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import api from './api'
import { authState, clearAuth, isAdmin, isMockMode, updateUser } from './auth'

const route = useRoute()
const router = useRouter()

const navs = [
  { to: '/bank', label: '题库' },
  { to: '/photo', label: '拍照搜题' },
  { to: '/practice', label: '练习' },
  { to: '/report', label: '每日报告' },
  { to: '/review', label: '今日复习' },
  { to: '/account', label: '账号' },
  { to: '/admin', label: '用户管理', admin: true },
  { to: '/admin/questions', label: '题库编辑', admin: true }
]

const isAuthPage = computed(() => route.meta.public === true)

function logout() {
  api.logout().catch(() => {}).finally(() => {
    clearAuth()
    router.replace('/login')
  })
}

onMounted(async () => {
  if (!authState.token || isMockMode()) return
  try {
    updateUser(await api.me())
  } catch {
    // 401 由 axios 拦截器统一跳登录
  }
})
</script>

<template>
  <RouterView v-if="isAuthPage" />
  <div v-else class="min-h-screen">
    <header class="bg-white border-b border-slate-200 sticky top-0 z-10">
      <div class="max-w-5xl mx-auto px-4 h-14 flex items-center gap-6">
        <span class="font-bold text-lg">考研高数学习平台</span>
        <nav class="flex gap-4">
          <template v-for="n in navs" :key="n.to">
            <RouterLink
              v-if="!n.admin || isAdmin()"
              :to="n.to"
              class="text-slate-600 hover:text-indigo-600"
              active-class="text-indigo-600 font-semibold"
            >
              {{ n.label }}
            </RouterLink>
          </template>
        </nav>
        <div class="ml-auto flex items-center gap-3 text-sm">
          <span v-if="isMockMode()" class="px-2 py-0.5 rounded bg-amber-50 text-amber-600 border border-amber-200">演示模式</span>
          <span class="text-slate-500">{{ authState.user?.username || (isMockMode() ? 'mock' : '') }}</span>
          <button
            v-if="!isMockMode()"
            class="text-slate-400 hover:text-rose-500 transition"
            @click="logout"
          >退出</button>
        </div>
      </div>
    </header>
    <main class="max-w-5xl mx-auto px-4 py-6">
      <RouterView />
    </main>
  </div>
</template>
