/**
 * 剧集 API - DramaBagus 对齐（`/api/dramas` → Java 3001）
 * 指令 #076：列表防抖+去重；详情短期缓存（变更后请 invalidateDramaCaches）
 */
import request from './request'
import { cacheAsync, clearCacheKeyPrefix, debounceAsync, deduplicateAsync } from '@/utils/requestOptimizer'

const dramaListKey = (params) => `drama:list:${JSON.stringify(params ?? {})}`
const dramaDetailKey = (id) => `drama:detail:${id}`

const fetchDramaList = (params) => request.get('/dramas', { params })
const fetchDramaDetail = (id) => request.get(`/dramas/${id}`)

/** 列表：防抖 + 去重（不缓存，避免 CRUD 后列表陈旧） */
export const getDramaList = debounceAsync(
  dramaListKey,
  280,
  deduplicateAsync(dramaListKey, fetchDramaList),
)

/** 详情：去重 + 5 分钟内存缓存 */
export const getDramaDetail = deduplicateAsync(
  dramaDetailKey,
  cacheAsync(dramaDetailKey, 5 * 60 * 1000, fetchDramaDetail),
)

/** 短剧写操作后调用，清除详情缓存 */
export function invalidateDramaCaches() {
  clearCacheKeyPrefix('drama:')
}

/** 与列表筛选一致（title / category_id / id / status），勿传 page/pageSize */
export function getDramaStats(params) {
  return request.get('/dramas/stats', { params })
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
