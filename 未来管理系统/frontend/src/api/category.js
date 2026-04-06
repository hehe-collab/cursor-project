/**
 * 分类 API — `GET /api/categories` 走 Java 3001
 *
 * 无 `page`/`pageSize` 时：`data` 为**数组**（短剧表单下拉等）。
 * 有分页参数时：`data` 为 `{ list, total, page, pageSize }`。
 */
import request from './request'

export function getCategories(params) {
  return request.get('/categories', { params })
}

export function getCategoryStats() {
  return request.get('/categories/stats')
}

export function getCategoryById(id) {
  return request.get(`/categories/${id}`)
}

export function createCategory(data) {
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
