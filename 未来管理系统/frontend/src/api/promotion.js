/**
 * 投放链接 API - DramaBagus 对齐
 */
import request from './request'

export function getPromotionList(params) {
  return request.get('/promotion', { params })
}

export function createPromotion(data) {
  return request.post('/promotion', data)
}

export function updatePromotion(id, data) {
  return request.put(`/promotion/${id}`, data)
}

export function deletePromotion(id) {
  return request.delete(`/promotion/${id}`)
}
