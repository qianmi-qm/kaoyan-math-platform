<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import api from './api'
import { authState, clearAuth, isAdmin, isMockMode, updateUser } from './auth'

const route = useRoute()
const router = useRouter()

const navOpen = ref(false)
const userOpen = ref(false)
const userMenu = ref(null)

const primaryNavs = [
  { to: '/bank', label: '题库', icon: '∑' },
  { to: '/photo', label: '拍照搜题', icon: '◉' },
  { to: '/practice', label: '练习', icon: '✎' },
  { to: '/report', label: '每日报告', icon: '◔' },
  { to: '/review', label: '今日复习', icon: '↻' }
]

const adminNavs = [
  { to: '/admin', label: '用户管理', icon: '◫' },
  { to: '/admin/questions', label: '题库编辑', icon: '✎' }
]

const isAuthPage = computed(() => route.meta.public === true)
const username = computed(() => authState.user?.username || (isMockMode() ? 'mock' : '同学'))
const initial = computed(() => username.value.slice(0, 1).toUpperCase())
const roleLabel = computed(() => (authState.user?.role === 'ADMIN' ? '管理员' : '学习者'))

function closeMenus() {
  navOpen.value = false
  userOpen.value = false
}

function logout() {
  closeMenus()
  api.logout().catch(() => {}).finally(() => {
    clearAuth()
    router.replace('/login')
  })
}

function handleDocumentClick(event) {
  if (userMenu.value && !userMenu.value.contains(event.target)) {
    userOpen.value = false
  }
}

watch(() => route.fullPath, closeMenus)

onMounted(async () => {
  document.addEventListener('click', handleDocumentClick)
  if (!authState.token || isMockMode()) return
  try {
    updateUser(await api.me())
  } catch {
    // 401 由 axios 拦截器统一跳登录
  }
})

onBeforeUnmount(() => document.removeEventListener('click', handleDocumentClick))
</script>

<template>
  <RouterView v-if="isAuthPage" />

  <div v-else class="app-bg relative min-h-screen">
    <div class="pointer-events-none absolute inset-x-0 top-0 h-72 bg-gradient-to-b from-indigo-100/40 to-transparent"></div>

    <header class="sticky top-0 z-30 border-b border-slate-200/70 bg-white/80 backdrop-blur-xl">
      <div class="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6">
        <RouterLink to="/bank" class="group flex shrink-0 items-center gap-2.5">
          <span class="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-xl font-serif text-white shadow-lg shadow-indigo-500/25 transition group-hover:scale-105">∫</span>
          <span class="leading-tight">
            <span class="block text-[15px] font-bold tracking-tight text-slate-900">研途高数</span>
            <span class="block text-[9px] font-semibold uppercase tracking-[0.22em] text-slate-400">Math Lab</span>
          </span>
        </RouterLink>

        <nav class="mx-auto hidden items-center gap-1 rounded-full border border-slate-200/70 bg-slate-50/80 p-1 lg:flex">
          <RouterLink
            v-for="n in primaryNavs"
            :key="n.to"
            :to="n.to"
            class="rounded-full px-3.5 py-1.5 text-[13px] font-medium text-slate-500 transition hover:text-slate-900"
            active-class="bg-white text-indigo-600 shadow-sm shadow-slate-200/70"
          >
            <span class="mr-1 text-[11px] opacity-60">{{ n.icon }}</span>{{ n.label }}
          </RouterLink>
        </nav>

        <div class="ml-auto flex items-center gap-2">
          <span v-if="isMockMode()" class="chip hidden bg-amber-50 text-amber-600 sm:inline-flex">演示模式</span>

          <div ref="userMenu" class="relative">
            <button
              class="flex items-center gap-2 rounded-full border border-slate-200/80 bg-white p-1 pr-2.5 transition hover:border-indigo-200 hover:shadow-sm"
              @click.stop="userOpen = !userOpen"
            >
              <span class="grid h-7 w-7 place-items-center rounded-full bg-gradient-to-br from-slate-700 to-slate-900 text-xs font-bold text-white">{{ initial }}</span>
              <span class="hidden text-left leading-tight sm:block">
                <span class="block text-xs font-semibold text-slate-700">{{ username }}</span>
                <span class="block text-[10px] text-slate-400">{{ roleLabel }}</span>
              </span>
              <svg class="h-3.5 w-3.5 text-slate-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
              </svg>
            </button>

            <div
              v-if="userOpen"
              class="absolute right-0 mt-2 w-56 overflow-hidden rounded-2xl border border-slate-200 bg-white p-2 shadow-xl shadow-slate-200/60 fade-up"
            >
              <div class="px-3 py-2">
                <div class="text-sm font-semibold text-slate-800">{{ username }}</div>
                <div class="text-xs text-slate-400">{{ roleLabel }} · {{ authState.user?.email || '未绑定邮箱' }}</div>
              </div>
              <div class="my-1 h-px bg-slate-100"></div>
              <RouterLink to="/account" class="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-50">
                <span class="text-slate-400">⚙</span>账号设置
              </RouterLink>
              <template v-if="isAdmin()">
                <RouterLink
                  v-for="n in adminNavs"
                  :key="n.to"
                  :to="n.to"
                  class="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"
                >
                  <span class="text-slate-400">{{ n.icon }}</span>{{ n.label }}
                </RouterLink>
              </template>
              <div class="my-1 h-px bg-slate-100"></div>
              <button class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-rose-600 hover:bg-rose-50" @click="logout">
                <span>↪</span>退出登录
              </button>
            </div>
          </div>

          <button class="btn-ghost lg:hidden" aria-label="打开菜单" @click.stop="navOpen = !navOpen">
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path v-if="!navOpen" stroke-linecap="round" d="M4 7h16M4 12h16M4 17h16" />
              <path v-else stroke-linecap="round" d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>
      </div>

      <div v-if="navOpen" class="border-t border-slate-100 bg-white px-4 py-3 lg:hidden">
        <div class="grid grid-cols-2 gap-1">
          <RouterLink
            v-for="n in primaryNavs"
            :key="n.to"
            :to="n.to"
            class="rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-50"
            active-class="bg-indigo-50 text-indigo-600"
          >{{ n.icon }} {{ n.label }}</RouterLink>
        </div>
        <div v-if="isAdmin()" class="mt-2 border-t border-slate-100 pt-2">
          <RouterLink
            v-for="n in adminNavs"
            :key="n.to"
            :to="n.to"
            class="block rounded-xl px-3 py-2 text-sm text-slate-500 hover:bg-slate-50"
          >{{ n.label }}</RouterLink>
        </div>
      </div>
    </header>

    <main class="relative mx-auto max-w-6xl px-4 py-7 sm:px-6 sm:py-9">
      <RouterView />
    </main>

    <footer class="relative mx-auto flex max-w-6xl flex-col gap-1 px-4 pb-8 pt-2 text-[11px] text-slate-400 sm:flex-row sm:items-center sm:justify-between sm:px-6">
      <span>研途高数 · 把每一道错题变成下一次得分</span>
      <span>v3.6 · 学习数据按账号隔离</span>
    </footer>
  </div>
</template>
