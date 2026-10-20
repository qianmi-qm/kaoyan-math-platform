<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import api from './api'
import { authState, clearAuth, isAdmin, isMockMode, updateUser } from './auth'

const route = useRoute()
const router = useRouter()

const userOpen = ref(false)
const userMenu = ref(null)

const primaryNavs = [
  { to: '/bank', label: '题库', icon: '∑' },
  { to: '/photo', label: '拍照搜题', icon: '◉' },
  { to: '/practice', label: '练习', icon: '✎' },
  { to: '/report', label: '每日报告', icon: '◔' },
  { to: '/review', label: '今日复习', icon: '↻' }
]

const mobileNavs = [
  { to: '/bank', label: '题库', icon: 'M4 5.5A2.5 2.5 0 016.5 3H20v16H6.5A2.5 2.5 0 004 21.5v-16z M8 7h8M8 11h6' },
  { to: '/photo', label: '拍照', icon: 'M4 8h3l1.5-2h7L17 8h3v11H4V8z M12 16a3 3 0 100-6 3 3 0 000 6z' },
  { to: '/practice', label: '练习', icon: 'M4 20l4-1 11-11-3-3L5 16l-1 4z M14 6l3 3' },
  { to: '/review', label: '复习', icon: 'M4 5v5h5 M5.5 10a7 7 0 101.2-4.2' },
  { to: '/account', label: '我的', icon: 'M12 12a4 4 0 100-8 4 4 0 000 8z M4 21a8 8 0 0116 0' }
]

const adminNavs = [
  { to: '/admin', label: '用户管理', icon: '◫' },
  { to: '/admin/questions', label: '题库编辑', icon: '✎' }
]

const isAuthPage = computed(() => route.meta.public === true)
const username = computed(() => authState.user?.username || (isMockMode() ? 'mock' : '同学'))
const initial = computed(() => username.value.slice(0, 1).toUpperCase())
const roleLabel = computed(() => (authState.user?.role === 'ADMIN' ? '管理员' : '学习者'))

function logout() {
  userOpen.value = false
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

watch(() => route.fullPath, () => { userOpen.value = false })

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

    <header class="sticky top-0 z-30 border-b border-slate-200/70 bg-white/85 backdrop-blur-xl">
      <div class="mx-auto flex h-14 max-w-6xl items-center gap-4 px-4 sm:h-16 sm:px-6">
        <RouterLink to="/bank" class="group flex shrink-0 items-center gap-2.5">
          <span class="grid h-8 w-8 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-lg font-serif text-white shadow-lg shadow-indigo-500/25 transition group-hover:scale-105 sm:h-9 sm:w-9 sm:text-xl">∫</span>
          <span class="leading-tight">
            <span class="block text-[14px] font-bold tracking-tight text-slate-900 sm:text-[15px]">研途高数</span>
            <span class="block text-[8px] font-semibold uppercase tracking-[0.22em] text-slate-400 sm:text-[9px]">Math Lab</span>
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
              class="flex items-center gap-2 rounded-full border border-slate-200/80 bg-white p-1 pr-2 transition hover:border-indigo-200 hover:shadow-sm sm:pr-2.5"
              aria-label="账号菜单"
              @click.stop="userOpen = !userOpen"
            >
              <span class="grid h-7 w-7 place-items-center rounded-full bg-gradient-to-br from-slate-700 to-slate-900 text-xs font-bold text-white">{{ initial }}</span>
              <span class="hidden text-left leading-tight sm:block">
                <span class="block max-w-24 truncate text-xs font-semibold text-slate-700">{{ username }}</span>
                <span class="block text-[10px] text-slate-400">{{ roleLabel }}</span>
              </span>
              <svg class="hidden h-3.5 w-3.5 text-slate-400 sm:block" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
              </svg>
            </button>

            <div
              v-if="userOpen"
              class="absolute right-0 mt-2 w-60 overflow-hidden rounded-2xl border border-slate-200 bg-white p-2 shadow-xl shadow-slate-200/60 fade-up"
            >
              <div class="px-3 py-2">
                <div class="truncate text-sm font-semibold text-slate-800">{{ username }}</div>
                <div class="truncate text-xs text-slate-400">{{ roleLabel }} · {{ authState.user?.email || '未绑定邮箱' }}</div>
              </div>
              <div class="my-1 h-px bg-slate-100"></div>
              <RouterLink to="/account" class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-50">
                <span class="text-slate-400">⚙</span>账号设置
              </RouterLink>
              <RouterLink to="/report" class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-50 lg:hidden">
                <span class="text-slate-400">◔</span>学习报告
              </RouterLink>
              <template v-if="isAdmin()">
                <div class="my-1 h-px bg-slate-100"></div>
                <RouterLink
                  v-for="n in adminNavs"
                  :key="n.to"
                  :to="n.to"
                  class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-50"
                >
                  <span class="text-slate-400">{{ n.icon }}</span>{{ n.label }}
                </RouterLink>
              </template>
              <div class="my-1 h-px bg-slate-100"></div>
              <button class="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-sm text-rose-600 hover:bg-rose-50" @click="logout">
                <span>↪</span>退出登录
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="relative mx-auto max-w-6xl px-4 pb-28 pt-5 sm:px-6 sm:py-9 lg:pb-9">
      <RouterView />
    </main>

    <footer class="relative mx-auto hidden max-w-6xl items-center justify-between px-6 pb-8 pt-2 text-[11px] text-slate-400 sm:flex">
      <span>研途高数 · 把每一道错题变成下一次得分</span>
      <span>v3.9 · 学习数据按账号隔离</span>
    </footer>

    <!-- 手机端底部导航 -->
    <nav class="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200/80 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur-xl lg:hidden">
      <div class="mx-auto grid h-[62px] max-w-lg grid-cols-5">
        <RouterLink
          v-for="n in mobileNavs"
          :key="n.to"
          :to="n.to"
          class="flex flex-col items-center justify-center gap-1 text-[10px] font-medium text-slate-400 transition active:scale-95"
          active-class="!text-indigo-600"
        >
          <svg class="h-[22px] w-[22px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path :d="n.icon" />
          </svg>
          <span>{{ n.label }}</span>
        </RouterLink>
      </div>
    </nav>
  </div>
</template>
