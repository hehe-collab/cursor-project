/**
 * 看板 API - DramaBagus 对齐
 */
import request from './request'

export function getDashboardStats(params) {
  return request.get('/dashboard/stats', { params })
}
