<script setup>
import { RouterLink, RouterView } from 'vue-router'

const navs = [
  { to: '/bank', label: '题库' },
  { to: '/practice', label: '练习' },
  { to: '/report', label: '每日报告' },
  { to: '/review', label: '今日复习' }
]
</script>

<template>
  <div class="min-h-screen">
    <header class="bg-white border-b border-slate-200 sticky top-0 z-10">
      <div class="max-w-5xl mx-auto px-4 h-14 flex items-center gap-6">
        <span class="font-bold text-lg">考研高数学习平台</span>
        <nav class="flex gap-4">
          <RouterLink
            v-for="n in navs"
            :key="n.to"
            :to="n.to"
            class="text-slate-600 hover:text-indigo-600"
            active-class="text-indigo-600 font-semibold"
          >
            {{ n.label }}
          </RouterLink>
        </nav>
      </div>
    </header>
    <main class="max-w-5xl mx-auto px-4 py-6">
      <RouterView />
    </main>
  </div>
</template>
