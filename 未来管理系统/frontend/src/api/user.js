/**
 * 用户 API - DramaBagus 对齐（列表/详情/统计走 Java `/api/users`）
 */
import request from './request'

export function getUserList(params) {
  return request.get('/users', { params })
}

/** 与列表相同筛选（勿传 page/pageSize）；含 start_date / end_date（YYYY-MM-DD） */
export function getUserStats(params) {
  return request.get('/users/stats', { params })
}

export function getUserById(id) {
  return request.get(`/users/${id}`)
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}
