/**
 * 导出工具：将表格数据导出为 CSV
 */
export function exportToCSV(data, columns, filename = 'export.csv') {
  if (!data?.length) return
  const headers = columns.map(c => (typeof c === 'object' ? c.label : c)).join(',')
  const rows = data.map(row =>
    columns.map(col => {
      const key = typeof col === 'object' ? col.prop : col
      let val = row[key]
      if (val == null) val = ''
      val = String(val).replace(/"/g, '""')
      return `"${val}"`
    }).join(',')
  )
  const csv = '\uFEFF' + [headers, ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
