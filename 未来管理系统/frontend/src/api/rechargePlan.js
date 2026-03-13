/**
 * 充值方案 API - DramaBagus 对齐
 */
import request from './request'

export function getRechargePlanList(params) {
  return request.get('/recharge-plan', { params })
}

export function createRechargePlan(data) {
  return request.post('/recharge-plan', data)
}

export function updateRechargePlan(id, data) {
  return request.put(`/recharge-plan/${id}`, data)
}

export function deleteRechargePlan(id) {
  return request.delete(`/recharge-plan/${id}`)
}
