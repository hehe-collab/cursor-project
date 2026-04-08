/**
 * 标签 API — `GET /api/tags` 走 Java 3001
 *
 * 无 `page`/`pageSize` 时：`data` 为**数组**（短剧表单下拉等）。
 * 有分页参数时：`data` 为 `{ list, total, page, pageSize }`。
 */
import request, { getWithCache, clearApiCacheKeyPrefix } from './request'

const TAG_GET_PREFIX = 'GET:/tags:'

export function getTags(params) {
  return getWithCache('/tags', params ?? {}, { ttl: 5 * 60 * 1000 })
}

export function getTagStats() {
  return request.get('/tags/stats')
}

export function getTagById(id) {
  return request.get(`/tags/${id}`)
}

export async function createTag(data) {
  const res = await request.post('/tags', data)
  if (res?.code === 0) clearApiCacheKeyPrefix(TAG_GET_PREFIX)
  return res
}

export async function updateTag(id, data) {
  const res = await request.put(`/tags/${id}`, data)
  if (res?.code === 0) clearApiCacheKeyPrefix(TAG_GET_PREFIX)
  return res
}

export async function deleteTag(id) {
  const res = await request.delete(`/tags/${id}`)
  if (res?.code === 0) clearApiCacheKeyPrefix(TAG_GET_PREFIX)
  return res
}
