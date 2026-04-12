/**
 * 权限判断工具函数
 */

const STORAGE_KEY = 'permissions'

export function getPermissions() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch {
    return []
  }
}

export function setPermissions(permissions) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(permissions || []))
}

export function clearPermissions() {
  localStorage.removeItem(STORAGE_KEY)
}

/** 是否为超级管理员（拥有所有权限）——优先看角色，避免 localStorage.permissions 未写入时按钮全被 v-permission 摘掉 */
export function isSuperAdmin() {
  const perms = getPermissions()
  if (perms.includes('*')) return true
  if (perms.length > 30) return true
  try {
    const u = JSON.parse(localStorage.getItem('user') || '{}')
    if (u.role === 'super_admin') return true
    if (u.role_obj?.code === 'super_admin') return true
    const ui = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (ui.role_obj?.code === 'super_admin') return true
  } catch {
    /* ignore */
  }
  return false
}

/** 是否有指定权限 */
export function hasPermission(permission) {
  if (!permission) return true
  if (isSuperAdmin()) return true
  return getPermissions().includes(permission)
}

/** 是否有任一权限 */
export function hasAnyPermission(permissionList) {
  if (!permissionList || permissionList.length === 0) return true
  return permissionList.some(p => hasPermission(p))
}

/** 是否有所有权限 */
export function hasAllPermissions(permissionList) {
  if (!permissionList || permissionList.length === 0) return true
  return permissionList.every(p => hasPermission(p))
}
