/**
 * 推广明细 — Java `/api/promotion-details`（#079/#080）
 * axios baseURL 已为 `/api`，此处路径勿再加 `/api`。
 */
import request from './request'

export function getPromotionDetails(params) {
  return request.get('/promotion-details', { params })
}

export function getProfitChart(promotionId, params) {
  const id = encodeURIComponent(promotionId)
  return request.get(`/promotion-details/${id}/profit-chart`, { params })
}

/** #086：当前筛选条件下多推广汇总利润图 */
export function getProfitChartAll(params) {
  return request.get('/promotion-details/profit-chart-all', { params })
}

export function syncTikTokData() {
  return request.post('/promotion-details/sync')
}

/** #082：投放媒体列表（ad_accounts） */
export function getPromotionPlatforms() {
  return request.get('/promotion-details/platforms')
}

/** #082：国家列表 { code, name } */
export function getPromotionCountries() {
  return request.get('/promotion-details/countries')
}
