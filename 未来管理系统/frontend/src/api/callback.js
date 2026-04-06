/**
 * 回传 API - DramaBagus 对齐
 */
import request from './request'

export function getCallbackConfigList(params) {
  return request.get('/callback/config', { params })
}

export function saveCallbackConfigRow(data) {
  return request.post('/callback/config', data)
}

export function updateCallbackConfigRow(id, data) {
  return request.put(`/callback/config/${id}`, data)
}

export function deleteCallbackConfigRow(id) {
  return request.delete(`/callback/config/${id}`)
}

export function batchDeleteCallbackConfig(ids) {
  return request.post('/callback/config/batch-delete', { ids })
}

export function getCallbackStats() {
  return request.get('/callback/stats')
}

export function getCallbackLogs(params) {
  return request.get('/callback/logs', { params })
}
