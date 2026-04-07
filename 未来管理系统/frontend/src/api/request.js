import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { monitorApiRequest } from '@/utils/performance'
import { monitorApiError } from '@/utils/errorMonitor'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

function markApiMonitored(config) {
  if (!config.metadata) config.metadata = {}
  config.metadata.apiMonitored = true
}

function reportApiTiming(config, success) {
  if (!import.meta.env.DEV || !config) return
  if (config.metadata?.apiMonitored) return
  markApiMonitored(config)
  const endTime = Date.now()
  const startTime = config.metadata?.startTime ?? endTime
  monitorApiRequest(config.url || '', startTime, endTime, success)
}

function reportApiBizFailure(config, message, data, status) {
  if (!import.meta.env.DEV || !config) return
  if (config.metadata?.apiMonitored) return
  markApiMonitored(config)
  const endTime = Date.now()
  const startTime = config.metadata?.startTime ?? endTime
  monitorApiRequest(config.url || '', startTime, endTime, false)
  monitorApiError(
    { message: message || '请求失败', response: { status, statusText: '', data } },
    config.url || 'unknown',
    config,
  )
}

request.interceptors.request.use((config) => {
  if (!config.metadata) config.metadata = {}
  config.metadata.startTime = Date.now()
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let is401Handling = false

function handle401Json(message) {
  if (!is401Handling) {
    is401Handling = true
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.error(message || '登录已过期')
    router.push('/login').finally(() => {
      is401Handling = false
    })
  }
}

request.interceptors.response.use(
  (res) => {
    if (res.config.responseType === 'blob') {
      const ct = (res.headers['content-type'] || '').toLowerCase()
      if (ct.includes('application/json')) {
        return res.data.text().then((text) => {
          let j
          try {
            j = JSON.parse(text)
          } catch {
            reportApiBizFailure(res.config, '请求失败', null, res.status)
            ElMessage.error('请求失败')
            return Promise.reject(new Error('请求失败'))
          }
          if (j.code === 401) {
            reportApiBizFailure(res.config, j.message, j, res.status)
            handle401Json(j.message)
            return Promise.reject(new Error(j.message))
          }
          if (j.code !== 0 && j.code !== undefined) {
            reportApiBizFailure(res.config, j.message || '请求失败', j, res.status)
            ElMessage.error(j.message || '请求失败')
            return Promise.reject(new Error(j.message))
          }
          reportApiTiming(res.config, true)
          return res.data
        })
      }
      reportApiTiming(res.config, true)
      return res
    }

    const { code, message } = res.data
    if (code === 401) {
      reportApiBizFailure(res.config, message, res.data, res.status)
      handle401Json(message)
      return Promise.reject(new Error(message))
    }
    if (code !== 0 && code !== undefined) {
      reportApiBizFailure(res.config, message || '请求失败', res.data, res.status)
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    reportApiTiming(res.config, true)
    return res.data
  },
  (err) => {
    const cfg = err.config
    if (import.meta.env.DEV && cfg && !cfg.metadata?.apiMonitored) {
      markApiMonitored(cfg)
      const endTime = Date.now()
      const startTime = cfg.metadata?.startTime ?? endTime
      monitorApiRequest(cfg.url || '', startTime, endTime, false)
      monitorApiError(err, cfg.url || 'unknown', cfg)
    }
    const res = err.response
    if (err.config?.responseType === 'blob' && res?.data instanceof Blob) {
      const ct = (res.headers['content-type'] || '').toLowerCase()
      if (ct.includes('application/json')) {
        return res.data.text().then((text) => {
          let msg = err.message || '网络错误'
          try {
            const j = JSON.parse(text)
            msg = j.message || msg
            if (j.code === 401) {
              handle401Json(j.message)
              return Promise.reject(err)
            }
          } catch (_) {
            /* keep msg */
          }
          ElMessage.error(msg)
          return Promise.reject(err)
        })
      }
    }
    ElMessage.error(res?.data?.message || err.message || '网络错误')
    return Promise.reject(err)
  },
)

export default request
