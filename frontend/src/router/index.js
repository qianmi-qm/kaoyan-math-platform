import { createRouter, createWebHistory } from 'vue-router'
import { isAdmin, isLoggedIn } from '../auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/bank' },
    { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
    { path: '/bank', component: () => import('../views/QuestionBankView.vue') },
    { path: '/practice', component: () => import('../views/PracticeView.vue') },
    { path: '/report', component: () => import('../views/ReportView.vue') },
    { path: '/review', component: () => import('../views/ReviewView.vue') },
    { path: '/account', component: () => import('../views/AccountView.vue') },
    { path: '/admin', component: () => import('../views/AdminUsersView.vue'), meta: { admin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/bank' },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.path === '/login' && isLoggedIn()) return { path: '/bank' }
    return true
  }
  if (!isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !isAdmin()) {
    return { path: '/bank' }
  }
  return true
})

export default router
