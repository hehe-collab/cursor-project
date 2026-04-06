/**
 * 充值记录 API - DramaBagus 对齐
 */
import request from './request'

export function getRechargeList(params) {
  return request.get('/recharge', { params })
}

/** 充值记录统计（Java `/api/recharge/stats`，与列表筛选参数一致，勿传 page/pageSize） */
export function getRechargeStats(params) {
  return request.get('/recharge/stats', { params })
}

export function getRechargeRecord(id) {
  return request.get(`/recharge/${id}`)
}

export function createRechargeRecord(data) {
  return request.post('/recharge', data)
}

export function updateRechargeRecord(id, data) {
  return request.put(`/recharge/${id}`, data)
}

export function deleteRechargeRecord(id) {
  return request.delete(`/recharge/${id}`)
}

/** 充值方案（Node `/api/recharge-plans`，指令 #027） */
export function getRechargePlans(params) {
  return request.get('/recharge-plans', { params })
}

export function createRechargePlan(data) {
  return request.post('/recharge-plans', data)
}

export function updateRechargePlan(data) {
  return request.put(`/recharge-plans/${data.id}`, data)
}

export function deleteRechargePlan(id) {
  return request.delete(`/recharge-plans/${id}`)
}

/** 充值方案组 — Node `/api/recharge-groups`（指令 #031） */
export function getRechargePlanGroups(params) {
  return request.get('/recharge-groups', { params })
}

/** 单条详情（含 pixel_token，列表已脱敏） */
export function getRechargePlanGroup(id) {
  return request.get(`/recharge-groups/${id}`)
}

export function createRechargePlanGroup(data) {
  return request.post('/recharge-groups', data)
}

export function updateRechargePlanGroup(data) {
  return request.put(`/recharge-groups/${data.id}`, data)
}

export function deleteRechargePlanGroup(id) {
  return request.delete(`/recharge-groups/${id}`)
}

/** 批量删除方案组，指令 #034 */
export function batchDeleteRechargePlanGroups(ids) {
  return request.post('/recharge-groups/batch-delete', { ids })
}

export function testRechargeGroupPixel(data) {
  return request.post('/recharge-groups/test-pixel', data)
}
