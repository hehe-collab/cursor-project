import * as XLSX from 'xlsx'

/**
 * 将对象数组导出为 .xlsx（首行为中文表头即对象的 key）
 */
export function exportJsonToXlsx(rows, sheetName, filename) {
  if (!rows?.length) {
    return false
  }
  const ws = XLSX.utils.json_to_sheet(rows)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, sheetName || 'Sheet1')
  const safeName = (filename || 'export').replace(/[/\\?%*:|"<>]/g, '_')
  const withExt = safeName.endsWith('.xlsx') ? safeName : `${safeName}.xlsx`
  XLSX.writeFile(wb, withExt)
  return true
}
