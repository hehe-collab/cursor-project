/**
 * 管理员操作日志 API
 */
import request from '@/api/request'

export function getAdminLogs(params) {
  return request.get('/admin-logs', { params })
}

export function getAdminLogStats(params) {
  return request.get('/admin-logs/stats/by-type', { params })
}
