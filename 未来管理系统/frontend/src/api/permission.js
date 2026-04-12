/**
 * 权限管理 API
 */
import request from '@/api/request'

/** 所有权限（按模块分组） */
export function getPermissions() {
  return request.get('/permissions')
}

/** 当前管理员的权限代码列表 */
export function getMyPermissions() {
  return request.get('/permissions/my')
}