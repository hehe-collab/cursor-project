import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
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
    router.push('/login').finally(() => { is401Handling = false })
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
            ElMessage.error('请求失败')
            return Promise.reject(new Error('请求失败'))
          }
          if (j.code === 401) {
            handle401Json(j.message)
            return Promise.reject(new Error(j.message))
          }
          if (j.code !== 0 && j.code !== undefined) {
            ElMessage.error(j.message || '请求失败')
            return Promise.reject(new Error(j.message))
          }
          return res.data
        })
      }
      return res
    }

    const { code, message } = res.data
    if (code === 401) {
      handle401Json(message)
      return Promise.reject(new Error(message))
    }
    if (code !== 0 && code !== undefined) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    return res.data
  },
  (err) => {
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
          } catch (_) {}
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
