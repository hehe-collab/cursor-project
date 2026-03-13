/**
 * 统计 API - DramaBagus 对齐
 */
import request from './request'

export function getStatsList(params) {
  return request.get('/stats', { params })
}
