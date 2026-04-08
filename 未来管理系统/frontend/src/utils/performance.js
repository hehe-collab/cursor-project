/**
 * 性能监控（指令 #077，仅开发环境由 main / request 调用）
 * 防抖/节流（指令 #091，lodash-es）
 */
import { debounce as _debounce, throttle as _throttle } from 'lodash-es'

/**
 * 页面加载（navigation timing + 尽力采集 FCP）
 */
export function monitorPageLoad() {
  if (typeof window === 'undefined' || !window.performance?.timing) {
    return
  }

  window.addEventListener('load', () => {
    setTimeout(() => {
      const timing = window.performance.timing
      const nav = timing.navigationStart || 0
      const metrics = {
        dnsTime: timing.domainLookupEnd - timing.domainLookupStart,
        tcpTime: timing.connectEnd - timing.connectStart,
        requestTime: timing.responseEnd - timing.requestStart,
        domParseTime: timing.domInteractive - timing.domLoading,
        domReadyTime: timing.domContentLoadedEventEnd - nav,
        loadTime: timing.loadEventEnd - nav,
        firstPaintTime: timing.responseEnd - timing.fetchStart,
        firstContentfulPaintTime: 0,
      }

      try {
        const po = new PerformanceObserver((list) => {
          for (const e of list.getEntries()) {
            if (e.name === 'first-contentful-paint') {
              metrics.firstContentfulPaintTime = e.startTime
            }
          }
        })
        po.observe({ type: 'paint', buffered: true })
      } catch {
        // ignore
      }

      console.group('📊 页面性能（Performance #077）')
      console.log('DOM 就绪(ms):', metrics.domReadyTime)
      console.log('load(ms):', metrics.loadTime)
      console.log('FP 近似(ms):', metrics.firstPaintTime)
      if (metrics.firstContentfulPaintTime) {
        console.log('FCP(ms):', metrics.firstContentfulPaintTime)
      }
      console.groupEnd()

      const prev = JSON.parse(localStorage.getItem('performanceMetrics') || '[]')
      prev.push({ timestamp: new Date().toISOString(), url: window.location.href, ...metrics })
      while (prev.length > 10) prev.shift()
      localStorage.setItem('performanceMetrics', JSON.stringify(prev))
    }, 0)
  })
}

/**
 * API 耗时
 */
export function monitorApiRequest(url, startTime, endTime, success = true) {
  const duration = endTime - startTime
  const style = success ? 'color:#67C23A;font-weight:bold;' : 'color:#F56C6C;font-weight:bold;'
  console.log(`%c[API] ${success ? '✓' : '✗'} ${url}`, style, `${duration}ms`)
  if (duration > 2000) {
    console.warn(`⚠️ 慢请求: ${url} ${duration}ms`)
  }

  const apiMetrics = JSON.parse(localStorage.getItem('apiMetrics') || '[]')
  apiMetrics.push({
    timestamp: new Date().toISOString(),
    url,
    duration,
    success,
  })
  while (apiMetrics.length > 50) apiMetrics.shift()
  localStorage.setItem('apiMetrics', JSON.stringify(apiMetrics))
}

export function monitorComponentRender(name, startTime, endTime) {
  const d = endTime - startTime
  console.log(`%c[组件] ${name}`, 'color:#409EFF;font-weight:bold;', `${d}ms`)
  if (d > 100) console.warn(`⚠️ 慢渲染: ${name} ${d}ms`)
}

export function getPerformanceReport() {
  const performanceMetrics = JSON.parse(localStorage.getItem('performanceMetrics') || '[]')
  const apiMetrics = JSON.parse(localStorage.getItem('apiMetrics') || '[]')

  console.group('📊 性能报告（#077）')
  if (performanceMetrics.length) {
    console.group('页面（最近 10 次）')
    console.table(
      performanceMetrics.map((m) => ({
        时间: new Date(m.timestamp).toLocaleString(),
        页面: (m.url || '').split('/').pop() || '/',
        load: m.loadTime,
        DOM: m.domReadyTime,
      })),
    )
    console.groupEnd()
  }
  if (apiMetrics.length) {
    console.group('API（最近 50 次）')
    const ok = apiMetrics.filter((x) => x.success).length
    const avg = apiMetrics.reduce((s, x) => s + x.duration, 0) / apiMetrics.length
    console.log('总数:', apiMetrics.length, '成功:', ok, '失败:', apiMetrics.length - ok, '平均ms:', avg.toFixed(1))
    const slow = apiMetrics.filter((x) => x.duration > 2000)
    if (slow.length) {
      console.table(
        slow.map((m) => ({
          时间: new Date(m.timestamp).toLocaleString(),
          URL: m.url,
          ms: m.duration,
        })),
      )
    }
    console.groupEnd()
  }
  console.groupEnd()
}

export function clearPerformanceData() {
  localStorage.removeItem('performanceMetrics')
  localStorage.removeItem('apiMetrics')
  console.log('已清除 performanceMetrics / apiMetrics')
}

export function debounce(func, wait = 300, options = {}) {
  return _debounce(func, wait, {
    leading: false,
    trailing: true,
    ...options,
  })
}

export function throttle(func, wait = 300, options = {}) {
  return _throttle(func, wait, {
    leading: true,
    trailing: true,
    ...options,
  })
}

export function rafThrottle(func) {
  let rafId = null
  return function rafThrottled(...args) {
    if (rafId !== null) return
    rafId = requestAnimationFrame(() => {
      func.apply(this, args)
      rafId = null
    })
  }
}

export function withMeasure(func, name = 'fn') {
  return async function measured(...args) {
    const start = performance.now()
    const result = await func.apply(this, args)
    const end = performance.now()
    if (import.meta.env.DEV) {
      console.log(`[Performance] ${name}: ${(end - start).toFixed(2)}ms`)
    }
    return result
  }
}

let navTimer = null

/** 路由跳转开始（beforeEach 调用） */
export function markNavigationStart() {
  navTimer = performance.now()
}

/** 路由就绪（afterEach 调用） */
export function markNavigationEnd(to) {
  if (navTimer == null) return
  const ms = performance.now() - navTimer
  navTimer = null
  if (import.meta.env.DEV) {
    console.log(`%c[Route] ${to.fullPath}`, 'color:#909399;font-weight:bold;', `${ms.toFixed(1)}ms`)
  }
}
