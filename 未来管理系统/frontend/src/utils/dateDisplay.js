/**
 * 管理后台列表时间统一展示：YYYY-MM-DD HH:mm:ss
 * 兼容 ISO 字符串、以及误写为 2026-03-10 07-21-20（时分秒用 -）的旧数据。
 */
export function formatDateTimeDisplay(value) {
  if (value == null || value === '') return '—'
  const str = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(str)) return str
  const broken = str.match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2})-(\d{2})-(\d{2})$/)
  if (broken) {
    return `${broken[1]} ${broken[2]}:${broken[3]}:${broken[4]}`
  }
  const d = new Date(str)
  if (!Number.isNaN(d.getTime())) {
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  return str
}
