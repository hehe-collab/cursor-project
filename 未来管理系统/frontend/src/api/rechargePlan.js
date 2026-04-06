/**
 * 充值方案 API - DramaBagus 对齐
 */
import request from './request'

export function getRechargePlanList(params) {
  return request.get('/recharge-plans', { params })
}

export function createRechargePlan(data) {
  return request.post('/recharge-plans', data)
}

export function updateRechargePlan(id, data) {
  return request.put(`/recharge-plans/${id}`, data)
}

export function deleteRechargePlan(id) {
  return request.delete(`/recharge-plans/${id}`)
}
