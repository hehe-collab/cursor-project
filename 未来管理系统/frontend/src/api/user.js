/**
 * 用户 API - DramaBagus 对齐
 */
import request from './request'

export function getUserList(params) {
  return request.get('/users', { params })
}
