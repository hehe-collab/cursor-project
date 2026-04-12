/**
 * 角色管理 API
 */
import request from '@/api/request'

export function getRoles() {
  return request.get('/roles')
}

export function getRoleById(id) {
  return request.get(`/roles/${id}`)
}

export function createRole(data) {
  return request.post('/roles', data)
}

export function updateRole(id, data) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id) {
  return request.delete(`/roles/${id}`)
}

/** 为角色分配权限 */
export function assignPermissions(roleId, permissionIds) {
  return request.post(`/roles/${roleId}/permissions`, { permission_ids: permissionIds })
}