import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import './assets/compact-theme.css'
import './assets/id-copy-row.css'
import './style.css'
import VueVirtualScroller from 'vue-virtual-scroller'
import VueLazyload from 'vue3-lazyload'
import App from './App.vue'
import router from './router'
import { monitorPageLoad, getPerformanceReport } from '@/utils/performance'
import { monitorGlobalError, getErrorReport } from '@/utils/errorMonitor'

/** #091：懒加载占位（内联 SVG，避免额外静态资源） */
const LAZY_LOADING =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBzdHJva2U9IiM0MDlFRkYiPjxnIGZpbGw9Im5vbmUiIGZpbGwtcnVsZT0iZXZlbm9kZCI+PGcgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMSAxKSIgc3Ryb2tlLXdpZHRoPSIyIj48Y2lyY2xlIHN0cm9rZS1vcGFjaXR5PSIuNSIgY3g9IjE4IiBjeT0iMTgiIHI9IjE4Ii8+PHBhdGggZD0iTTM2IDE4YzAtOS45NC04LjA2LTE4LTE4LTE4Ij48YW5pbWF0ZVRyYW5zZm9ybSBhdHRyaWJ1dGVOYW1lPSJ0cmFuc2Zvcm0iIHR5cGU9InJvdGF0ZSIgZnJvbT0iMCAxOCAxOCIgdG89IjM2MCAxOCAxOCIgZHVyPSIxcyIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiLz48L3BhdGg+PC9nPjwvZz48L3N2Zz4='
const LAZY_ERROR =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiNmNWY1ZjUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZG9taW5hbnQtYmFzZWxpbmU9Im1pZGRsZSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzk5OSIgZm9udC1zaXplPSIxMCI+RXJyb3I8L3RleHQ+PC9zdmc+'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn, size: 'small', zIndex: 3000 })
app.use(router)
app.use(VueVirtualScroller)
app.use(VueLazyload, {
  loading: LAZY_LOADING,
  error: LAZY_ERROR,
})

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
