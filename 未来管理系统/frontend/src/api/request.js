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
request.interceptors.response.use(
  (res) => {
    const { code, message } = res.data
    if (code === 401) {
      if (!is401Handling) {
        is401Handling = true
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        ElMessage.error(message || '登录已过期')
        router.push('/login').finally(() => { is401Handling = false })
      }
      return Promise.reject(new Error(message))
    }
    if (code !== 0 && code !== undefined) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    return res.data
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request
