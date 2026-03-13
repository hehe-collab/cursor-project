/**
 * 标题包 API - DramaBagus 对齐
 */
import request from './request'

export function getTitlePackList(params) {
  return request.get('/title-pack', { params })
}

export function createTitlePack(data) {
  return request.post('/title-pack', data)
}

export function updateTitlePack(id, data) {
  return request.put(`/title-pack/${id}`, data)
}

export function deleteTitlePack(id) {
  return request.delete(`/title-pack/${id}`)
}
