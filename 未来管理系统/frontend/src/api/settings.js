/**
 * 系统设置 — `GET|POST|PUT /api/settings` 走 Java 3001（表 `settings`：`key_name`/`value`）。
 */
import request from './request'

/** @param {Record<string, string>} [params] 如 `{ group: 'basic' }`（当前后端无分组列，忽略后仍返回全部） */
export function getSettings(params) {
  return request.get('/settings', { params })
}

export function getSettingByKey(key) {
  return request.get(`/settings/${encodeURIComponent(key)}`)
}

/** 批量保存（扁平 key-value，与 `Settings.vue` 中 `POST` 一致） */
export function saveSettings(data) {
  return request.post('/settings', data)
}

/** 批量更新（`PUT`，与指令一致） */
export function batchUpdateSettings(data) {
  return request.put('/settings', data)
}

export function updateSetting(key, value) {
  return request.put(`/settings/${encodeURIComponent(key)}`, { value })
}
