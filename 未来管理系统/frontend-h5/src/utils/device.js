const DEVICE_ID_KEY = 'hks_device_id'
const ATTRIBUTION_KEY = 'hks_attribution'

/** 获取或生成设备唯一ID，存入 localStorage */
export function getDeviceId() {
  let id = localStorage.getItem(DEVICE_ID_KEY)
  if (!id) {
    id = 'dev_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem(DEVICE_ID_KEY, id)
  }
  return id
}

/** 从 URL 参数中读取归因信息并持久化 */
export function captureAttribution() {
  const params = new URLSearchParams(window.location.search)
  const promoId = params.get('promo_id')
  const clickId = params.get('click_id') || params.get('ttclid')
  const utmSource = params.get('utm_source')
  const utmMedium = params.get('utm_medium')
  const utmCampaign = params.get('utm_campaign')
  const utmId = params.get('utm_id')
  const adId = params.get('ad_id')

  // 只要有 promo_id 就更新归因，避免直接访问时覆盖已有数据
  if (promoId) {
    const attr = { promoId, clickId, utmSource, utmMedium, utmCampaign, utmId, adId, ts: Date.now() }
    localStorage.setItem(ATTRIBUTION_KEY, JSON.stringify(attr))
    return attr
  }

  const saved = localStorage.getItem(ATTRIBUTION_KEY)
  return saved ? JSON.parse(saved) : {}
}

/** 读取已保存的归因信息 */
export function getAttribution() {
  const saved = localStorage.getItem(ATTRIBUTION_KEY)
  return saved ? JSON.parse(saved) : {}
}
