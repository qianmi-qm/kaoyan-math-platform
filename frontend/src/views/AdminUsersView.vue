<script setup>
import { onMounted, ref } from 'vue'
import api from '../api'
import { authState } from '../auth'

const users = ref([])
const error = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    users.value = await api.adminUsers()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function toggleRole(user) {
  const next = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    await api.updateUserRole(user.id, next)
    await load()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-lg font-bold">用户管理</h2>
        <p class="text-sm text-slate-500 mt-1">管理员可调整其他用户的角色；不能修改自己。</p>
      </div>
      <button class="px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-slate-50" @click="load">刷新</button>
    </div>

    <div v-if="error" class="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm">{{ error }}</div>
    <div v-if="loading" class="text-center py-10 text-slate-400">加载中...</div>

    <div v-else class="bg-white rounded-xl border border-slate-200 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-slate-50 text-slate-500">
          <tr>
            <th class="text-left px-4 py-3 font-medium">ID</th>
            <th class="text-left px-4 py-3 font-medium">用户名</th>
            <th class="text-left px-4 py-3 font-medium">角色</th>
            <th class="text-right px-4 py-3 font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id" class="border-t border-slate-100">
            <td class="px-4 py-3 text-slate-400">{{ u.id }}</td>
            <td class="px-4 py-3 text-slate-700">{{ u.username }}</td>
            <td class="px-4 py-3">
              <span :class="u.role === 'ADMIN' ? 'bg-indigo-50 text-indigo-600' : 'bg-slate-100 text-slate-500'"
                    class="px-2 py-0.5 rounded text-xs font-medium">{{ u.role }}</span>
            </td>
            <td class="px-4 py-3 text-right">
              <button
                v-if="u.id !== authState.user?.id"
                @click="toggleRole(u)"
                class="text-xs text-indigo-600 hover:text-indigo-800"
              >{{ u.role === 'ADMIN' ? '降为 USER' : '提升为 ADMIN' }}</button>
              <span v-else class="text-xs text-slate-300">当前账号</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
