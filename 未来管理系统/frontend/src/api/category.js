/**
 * 分类管理 API
 * GET|POST|PUT|DELETE /api/categories
 */
import request from './request'

/** 获取分类列表（无分页参数时返回数组） */
export function getCategories(params) {
  return request.get('/categories', { params })
}

/** 获取单个分类 */
export function getCategoryById(id) {
  return request.get(`/categories/${id}`)
}

/** 创建分类 */
export function createCategory(data) {
  return request.post('/categories', data)
}

/** 更新分类 */
export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

/** 删除分类 */
export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}

/** 批量更新排序 */
export function batchSortCategories(ids) {
  return request.post('/categories/batch-sort', { ids })
}

/** 获取分类统计 */
export function getCategoryStats() {
  return request.get('/categories/stats')
}
