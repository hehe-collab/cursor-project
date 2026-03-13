/**
 * 回传 API - DramaBagus 对齐
 */
import request from './request'

export function getCallbackConfig() {
  return request.get('/callback/config')
}

export function saveCallbackConfig(data) {
  return request.post('/callback/config', data)
}

export function getCallbackLogs(params) {
  return request.get('/callback/logs', { params })
}
