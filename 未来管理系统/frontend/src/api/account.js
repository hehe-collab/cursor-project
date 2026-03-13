/**
 * 账户管理 API - DramaBagus 对齐
 */
import request from './request'

export function getAccountList(params) {
  return request.get('/account', { params })
}

export function createAccount(data) {
  return request.post('/account', data)
}

export function updateAccount(id, data) {
  return request.put(`/account/${id}`, data)
}

export function deleteAccount(id) {
  return request.delete(`/account/${id}`)
}
