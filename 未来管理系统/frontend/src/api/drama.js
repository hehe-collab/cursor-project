/**
 * 剧集 API - DramaBagus 对齐（`/api/dramas` → Java 3001）
 */
import request from './request'

export function getDramaList(params) {
  return request.get('/dramas', { params })
}

/** 与列表筛选一致（title / category_id / id / status），勿传 page/pageSize */
export function getDramaStats(params) {
  return request.get('/dramas/stats', { params })
}

export function getDramaDetail(id) {
  return request.get(`/dramas/${id}`)
}

/** 分集列表：`{ list, total }` */
export function getDramaEpisodes(dramaId) {
  return request.get(`/dramas/${dramaId}/episodes`)
}

export function createDramaEpisode(dramaId, data) {
  return request.post(`/dramas/${dramaId}/episodes`, data)
}

export function updateDramaEpisode(dramaId, episodeId, data) {
  return request.put(`/dramas/${dramaId}/episodes/${episodeId}`, data)
}

export function deleteDramaEpisode(dramaId, episodeId) {
  return request.delete(`/dramas/${dramaId}/episodes/${episodeId}`)
}

export function createDrama(data) {
  return request.post('/dramas', data)
}

export function updateDrama(id, data) {
  return request.put(`/dramas/${id}`, data)
}

export function deleteDrama(id) {
  return request.delete(`/dramas/${id}`)
}

/** @deprecated 使用 getDramaEpisodes(dramaId)；旧路径 `/dramas/episodes` 不再使用 */
export function getEpisodeList(params) {
  const dramaId = params?.drama_id ?? params?.dramaId
  if (dramaId == null) {
    return Promise.reject(new Error('getEpisodeList 需传 drama_id'))
  }
  return getDramaEpisodes(dramaId)
}

/** @deprecated 使用 updateDramaEpisode(dramaId, episodeId, data) */
export function updateEpisode(id, data) {
  const dramaId = data?.drama_id ?? data?.dramaId
  if (dramaId == null) {
    return Promise.reject(new Error('updateEpisode 请在 data 中带上 drama_id'))
  }
  return updateDramaEpisode(dramaId, id, data)
}
