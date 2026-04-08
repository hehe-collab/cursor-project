/**
 * 分类 API — `GET /api/categories` 走 Java 3001
 *
 * 无 `page`/`pageSize` 时：`data` 为**数组**（短剧表单下拉等）。
 * 有分页参数时：`data` 为 `{ list, total, page, pageSize }`。
 */
import request, { getWithCache, clearApiCacheKeyPrefix } from './request'

const CAT_GET_PREFIX = 'GET:/categories:'

export function getCategories(params) {
  return getWithCache('/categories', params ?? {}, { ttl: 5 * 60 * 1000 })
}

export function getCategoryStats() {
  return request.get('/categories/stats')
}

export function getCategoryById(id) {
  return request.get(`/categories/${id}`)
}

export async function createCategory(data) {
  const res = await request.post('/categories', data)
  if (res?.code === 0) clearApiCacheKeyPrefix(CAT_GET_PREFIX)
  return res
}

export async function updateCategory(id, data) {
  const res = await request.put(`/categories/${id}`, data)
  if (res?.code === 0) clearApiCacheKeyPrefix(CAT_GET_PREFIX)
  return res
}

export async function deleteCategory(id) {
  const res = await request.delete(`/categories/${id}`)
  if (res?.code === 0) clearApiCacheKeyPrefix(CAT_GET_PREFIX)
  return res
}
