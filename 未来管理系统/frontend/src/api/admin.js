/**
 * 管理员管理 API
 */
import request from '@/api/request'

export function getAdmins() {
  return request.get('/admins')
}

export function getAdminById(id) {
  return request.get(`/admins/${id}`)
}

export function createAdmin(data) {
  return request.post('/admins', data)
}

export function updateAdmin(id, data) {
  return request.put(`/admins/${id}`, data)
}

export function deleteAdmin(id) {
  return request.delete(`/admins/${id}`)
}

/** 重置管理员密码 */
export function resetAdminPassword(id, password) {
  return request.post(`/admins/${id}/reset-password`, { password })
}