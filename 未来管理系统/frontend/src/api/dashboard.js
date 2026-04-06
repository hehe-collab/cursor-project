/**
 * 看板 API — Java：`/api/dashboard`（`vite` 已代理 3001）
 *
 * - `GET /dashboard` 与 `GET /dashboard/stats` 聚合一致；无日期参数时默认近 7 天。
 * - `GET /dashboard/trends`：user | recharge | view。
 */
import request from './request'

/** @param {object} [params] `start_date` / `end_date`（YYYY-MM-DD） */
export function getDashboardSummary(params) {
  return request.get('/dashboard', { params })
}

export function getDashboardStats(params) {
  return request.get('/dashboard/stats', { params })
}

/** @param {object} params `type`：user | recharge | view；`days`：默认 7 */
export function getDashboardTrends(params) {
  return request.get('/dashboard/trends', { params })
}
