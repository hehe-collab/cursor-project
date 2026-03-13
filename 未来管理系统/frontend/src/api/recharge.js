/**
 * 充值记录 API - DramaBagus 对齐
 */
import request from './request'

export function getRechargeList(params) {
  return request.get('/recharge', { params })
}
