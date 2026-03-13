/**
 * 充值方案组 API - DramaBagus 对齐
 */
import request from './request'

export function getRechargeGroupList(params) {
  return request.get('/recharge-group', { params })
}

export function getPlanList(params = {}) {
  return request.get('/recharge-plan', { params: { pageSize: 1000, ...params } })
}

export function createRechargeGroup(data) {
  return request.post('/recharge-group', data)
}

export function updateRechargeGroup(id, data) {
  return request.put(`/recharge-group/${id}`, data)
}

export function deleteRechargeGroup(id) {
  return request.delete(`/recharge-group/${id}`)
}
