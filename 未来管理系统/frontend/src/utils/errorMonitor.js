/**
 * 错误监控（指令 #077，开发环境）
 */

function storeError(errorInfo) {
  try {
    const errors = JSON.parse(localStorage.getItem('errorLogs') || '[]')
    errors.push(errorInfo)
    while (errors.length > 100) errors.shift()
    localStorage.setItem('errorLogs', JSON.stringify(errors))
  } catch (e) {
    console.error('storeError failed', e)
  }
}

/**
 * window error：区分 JS 异常与静态资源加载失败（捕获阶段）
 */
export function monitorGlobalError() {
  window.addEventListener(
    'error',
    (event) => {
      const t = event.target
      if (t && t !== window && (t.tagName === 'IMG' || t.tagName === 'SCRIPT' || t.tagName === 'LINK')) {
        const errorInfo = {
          type: 'Resource Error',
          message: `资源加载失败: ${t.src || t.href || ''}`,
          element: t.tagName,
          timestamp: new Date().toISOString(),
          url: window.location.href,
        }
        console.group('❌ 资源错误')
        console.error(errorInfo.message)
        console.groupEnd()
        storeError(errorInfo)
        return
      }

      const errorInfo = {
        type: 'JavaScript Error',
        message: event.error?.message || event.message,
        stack: event.error?.stack || '',
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        timestamp: new Date().toISOString(),
        url: window.location.href,
      }
      console.group('❌ JS 错误')
      console.error(errorInfo.message, errorInfo.filename, `${errorInfo.lineno}:${errorInfo.colno}`)
      console.groupEnd()
      storeError(errorInfo)
    },
    true,
  )

  window.addEventListener('unhandledrejection', (event) => {
    const r = event.reason
    const errorInfo = {
      type: 'Promise Rejection',
      message: r?.message || String(r),
      stack: r?.stack || '',
      timestamp: new Date().toISOString(),
      url: window.location.href,
    }
    console.group('❌ Promise')
    console.error(errorInfo.message)
    console.groupEnd()
    storeError(errorInfo)
  })
}

export function monitorApiError(error, url, config = {}) {
  const errorInfo = {
    type: 'API Error',
    message: error.message,
    url,
    method: (config?.method || 'get').toUpperCase(),
    status: error.response?.status,
    statusText: error.response?.statusText,
    data: error.response?.data,
    timestamp: new Date().toISOString(),
    pageUrl: window.location.href,
  }
  console.group('❌ API 错误')
  console.error(url, errorInfo.status, errorInfo.message)
  console.groupEnd()
  storeError(errorInfo)
}

export function getErrorReport() {
  const errors = JSON.parse(localStorage.getItem('errorLogs') || '[]')
  if (!errors.length) {
    console.log('暂无 errorLogs')
    return
  }
  console.group('📊 错误报告')
  const types = [...new Set(errors.map((e) => e.type))]
  console.log('条数:', errors.length, '类型:', types.join(', '))
  console.table(
    errors.slice(-10).map((e) => ({
      时间: new Date(e.timestamp).toLocaleString(),
      类型: e.type,
      消息: String(e.message).slice(0, 80),
    })),
  )
  console.groupEnd()
}

export function clearErrorLogs() {
  localStorage.removeItem('errorLogs')
  console.log('已清除 errorLogs')
}
