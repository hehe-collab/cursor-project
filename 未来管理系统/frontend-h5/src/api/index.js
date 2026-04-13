import axios from 'axios'
import { getDeviceId, getAttribution } from '@/utils/device'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  config.headers['X-Device-Id'] = getDeviceId()
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => Promise.reject(err),
)

/** 根据 promo_id 获取剧集信息（免费集数、方案组、视频列表） */
export function getDramaByPromo(promoId) {
  return http.get('/h5/drama', { params: { promo_id: promoId } })
}

/** 获取指定集的播放地址（后端校验金豆余额） */
export function getPlayUrl(promoId, episode) {
  return http.get('/h5/play', { params: { promo_id: promoId, episode } })
}

/** 获取用户金豆余额和套餐列表 */
export function getUserInfo() {
  return http.get('/h5/user')
}

/** 发起支付（返回 Stripe clientSecret） */
export function createPayment(planId, promoId) {
  const attr = getAttribution()
  return http.post('/h5/pay', { plan_id: planId, promo_id: promoId, attribution: attr })
}
