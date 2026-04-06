/**
 * 标签 API — `GET /api/tags` 走 Java 3001
 *
 * 无 `page`/`pageSize` 时：`data` 为**数组**（短剧表单下拉等）。
 * 有分页参数时：`data` 为 `{ list, total, page, pageSize }`。
 */
import request from './request'

export function getTags(params) {
  return request.get('/tags', { params })
}

export function getTagStats() {
  return request.get('/tags/stats')
}

export function getTagById(id) {
  return request.get(`/tags/${id}`)
}

export function createTag(data) {
  return request.post('/tags', data)
}

export function updateTag(id, data) {
  return request.put(`/tags/${id}`, data)
}

export function deleteTag(id) {
  return request.delete(`/tags/${id}`)
}
