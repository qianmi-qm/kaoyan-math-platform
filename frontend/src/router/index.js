import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/bank' },
    { path: '/bank', component: () => import('../views/QuestionBankView.vue') },
    { path: '/practice', component: () => import('../views/PracticeView.vue') },
    { path: '/report', component: () => import('../views/ReportView.vue') },
    { path: '/review', component: () => import('../views/ReviewView.vue') }
  ]
})

export default router
