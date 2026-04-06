/**
 * 跨模块通用 API（指令 #026）
 */
import request from './request'

/** 广告账户已录入的国家列表（去重），GET /api/accounts/countries */
export function getAccountCountryList() {
  return request({
    url: '/accounts/countries',
    method: 'get',
  })
}
