/**
 * 剧集 API - DramaBagus 对齐
 */
import request from './request'

export function getDramaList(params) {
  return request.get('/dramas', { params })
}

export function getDramaDetail(id) {
  return request.get(`/dramas/${id}`)
}

export function getEpisodeList(params) {
  return request.get('/dramas/episodes', { params })
}

export function updateDrama(id, data) {
  return request.put(`/dramas/${id}`, data)
}

export function updateEpisode(id, data) {
  return request.put(`/dramas/episodes/${id}`, data)
}
