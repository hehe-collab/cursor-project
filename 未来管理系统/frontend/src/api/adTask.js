/**
 * 广告任务 API - DramaBagus 对齐
 */
import request from './request'

export function getAdTaskList(params) {
  return request.get('/ad-task', { params })
}

export function createAdTask(data) {
  return request.post('/ad-task', data)
}

export function updateAdTask(id, data) {
  return request.put(`/ad-task/${id}`, data)
}

export function deleteAdTask(id) {
  return request.delete(`/ad-task/${id}`)
}
