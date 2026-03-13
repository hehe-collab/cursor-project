/**
 * 广告素材 API - DramaBagus 对齐
 */
import request from './request'

export function getAdMaterialList(params) {
  return request.get('/ad-material', { params })
}

export function createAdMaterial(data) {
  return request.post('/ad-material', data)
}

export function updateAdMaterial(id, data) {
  return request.put(`/ad-material/${id}`, data)
}

export function deleteAdMaterial(id) {
  return request.delete(`/ad-material/${id}`)
}
