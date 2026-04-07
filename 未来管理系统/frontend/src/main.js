import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './assets/compact-theme.css'
import './assets/id-copy-row.css'
import './style.css'
import App from './App.vue'
import router from './router'
import { monitorPageLoad, getPerformanceReport } from '@/utils/performance'
import { monitorGlobalError, getErrorReport } from '@/utils/errorMonitor'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn, size: 'small', zIndex: 3000 })
app.use(router)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

if (import.meta.env.DEV) {
  monitorPageLoad()
  monitorGlobalError()
  window.getPerformanceReport = getPerformanceReport
  window.getErrorReport = getErrorReport
  console.info('%c#077 性能/错误监控已启用', 'color:#67C23A;font-weight:bold;')
  console.info('控制台执行 getPerformanceReport() / getErrorReport()')
}

app.mount('#app')
