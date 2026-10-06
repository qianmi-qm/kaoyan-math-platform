import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { http } from './api'
import { initMock } from './mock'
import './style.css'
import 'katex/dist/katex.min.css'

// 启用 mock 数据：URL 加 ?mock=1 或 localStorage.mock='1'
initMock(http)

createApp(App).use(router).mount('#app')
