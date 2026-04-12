/**
 * 标签管理 API
 * GET|POST|PUT|DELETE /api/tags
 */
import request from './request'

/** 获取标签列表（无分页参数时返回数组） */
export function getTags(params) {
  return request.get('/tags', { params })
}

/** 获取单个标签 */
export function getTagById(id) {
  return request.get(`/tags/${id}`)
}

/** 创建标签 */
export function createTag(data) {
  return request.post('/tags', data)
}

/** 更新标签 */
export function updateTag(id, data) {
  return request.put(`/tags/${id}`, data)
}

/** 删除标签 */
export function deleteTag(id) {
  return request.delete(`/tags/${id}`)
}

/** 批量设置热门 */
export function batchSetHot(ids, isHot) {
  return request.post('/tags/batch-hot', { ids, is_hot: isHot })
}

/** 获取标签统计 */
export function getTagStats() {
  return request.get('/tags/stats')
}
