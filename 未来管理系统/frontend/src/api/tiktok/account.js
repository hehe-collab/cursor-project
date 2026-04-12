/**
 * TikTok 账户 API（与后端 TikTokAccountController 对齐）
 * 基础路径：/api/tiktok/accounts
 */
import request from '@/api/request'

/**
 * 查询账户列表（不分页，返回全部）
 * @param {Object} params
 * @param {string} [params.status] - active | inactive
 */
export function getAccountList(params = {}) {
  return request.get('/tiktok/accounts', { params })
}

/**
 * 按数字主键 ID 获取账户详情
 * @param {number} id
 */
export function getAccountById(id) {
  return request.get(`/tiktok/accounts/${id}`)
}

/**
 * 按 advertiser_id 字符串获取账户
 * @param {string} advertiserId
 */
export function getAccountByAdvertiserId(advertiserId) {
  return request.get(`/tiktok/accounts/advertiser/${advertiserId}`)
}
