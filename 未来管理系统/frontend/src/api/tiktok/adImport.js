/**
 * TikTok 广告导入 API
 * 支持三层级统一导入：一张 Excel 包含广告系列 + 广告组 + 广告，通过 level 列区分
 */
import request from '@/api/request'

/**
 * 分页查询导入记录
 */
export function getImportList(params = {}) {
  return request.get('/tiktok/excel-imports', { params })
}

/**
 * 按 ID 查询单条导入记录
 */
export function getImportById(id) {
  return request.get(`/tiktok/excel-imports/${id}`)
}

/**
 * 上传 Excel 并处理（三层级统一）
 * @param {FormData} formData
 * @param {Function} onUploadProgress - 上传进度回调
 * @param {string} formData.get('file') - Excel 文件
 * @param {string} formData.get('importMode') - single | multiple
 * @param {string} [formData.get('advertiserId')] - 单账户模式必填
 */
export function uploadExcel(formData, onUploadProgress) {
  return request.post('/tiktok/excel-imports', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress,
  })
}

/**
 * 删除导入记录
 */
export function deleteImport(id) {
  return request.delete(`/tiktok/excel-imports/${id}`)
}

/**
 * 下载统一三层级 Excel 模板
 * @param {string} importMode - single | multiple
 * @returns {Promise<Blob>}
 */
export function downloadTemplate(importMode = 'single') {
  return request.get('/tiktok/excel-imports/template', {
    params: { importMode },
    responseType: 'blob',
  }).then((res) => {
    if (res?.data instanceof Blob) {
      return res.data
    }
    if (res instanceof Blob) {
      return res
    }
    return res
  })
}
